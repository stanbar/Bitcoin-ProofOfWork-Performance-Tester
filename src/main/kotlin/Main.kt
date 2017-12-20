/*
 * Copyright (c) 2017 Stanislaw stasbar Baranski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.fusesource.jansi.Ansi.ansi
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.IntStream
import kotlin.system.measureNanoTime


data class ProofOfWork(val hash_result: BigInteger, val nonce: Long);

val max_nonce = 1L shl 32 //4 Billion

val hashCounter = AtomicLong(0)

var startTime: Long = 0

fun main(args: Array<String>) {
    //AnsiConsole.systemInstall()
    //Difficult from 0 to 31 bits
    //AnsiConsole.systemInstall()

    startTime = System.nanoTime()
    IntStream.range(0, 31)
            .parallel()
            .forEach {
                testForDifficulty(it)
            }

}


fun testForDifficulty(difficultyBits: Int) {
    val message = ansi().reset()
            .fgBrightCyan()
            .a("[%03d bits]".format(difficultyBits))

    var proofOfWork: ProofOfWork? = null
    val elapsedTime = measureNanoTime {
        //Make a new block which includes the hash from previous block
        // we fake a block of transactions - just a string
        val newBlock = "test block with transactions" + difficultyBits

        //Find a valid nonce for that block
        proofOfWork = proofOfWork(newBlock, difficultyBits)

    }

    if (elapsedTime > 0) {
        //Estimates hashes per second
        hashCounter.addAndGet(proofOfWork!!.nonce)

        val hashPower = proofOfWork!!.nonce / elapsedTime.toDouble() * 1000000000.0

        message.fgBrightRed().a("[${humanReadableByteCount((hashPower.toLong()))}]")
    }
    val hashPower = hashCounter.get().toDouble() / (System.nanoTime() - startTime) * 1000000000

    message.fgRed().a(" [Tot: ${humanReadableByteCount((hashPower.toLong()))}]")
    message.fgBrightMagenta().a("[%.4f s]".format(elapsedTime / 1000000000.0))
    message.fgBrightGreen().a("[Nonce %d]".format(proofOfWork!!.nonce))
    message.fgDefault().a("[Hash ${proofOfWork!!.hash_result.toString(16)}]")
    println(message)
}

fun humanReadableByteCount(bytes: Long, si: Boolean = true): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return bytes.toString() + " H/z"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sH/s", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

fun proofOfWork(header: String, difficultyBits: Int): ProofOfWork {
    //Calculate the difficulty target
    val target = BigInteger.valueOf(2).pow(256 - difficultyBits)

    var hashResult: BigInteger = BigInteger.ZERO
    for (nonce in 0..max_nonce) {
        hashResult = sha256(header + nonce.toString())

        //check if it's a valid result, below the target
        if (hashResult < target)
            return ProofOfWork(hashResult, nonce)

    }

    print("Failed after %d".format(max_nonce))
    return ProofOfWork(hashResult, max_nonce)

}


fun sha256(base: String): BigInteger {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(base.toByteArray(charset("UTF-8")))
    return BigInteger(1, hash)
}

