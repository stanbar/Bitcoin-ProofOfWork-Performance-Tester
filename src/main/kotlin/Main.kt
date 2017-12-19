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
import kotlin.system.measureNanoTime

data class ProofOfWork(val hash_result: BigInteger, val nonce: Long);

val max_nonce = 1L shl 32 //4 Billion


fun main(args: Array<String>) {
    var nonce = 0L
    var hashResult = BigInteger.ZERO

    //Difficult from 0 to 31 bits
    for (difficultyBits in 0..31) {
        // 2 ^ difficultyBits = 1 << difficultyBits -1
        val difficulty = 1 shl difficultyBits
        val message = ansi().reset().fgBrightCyan().a("Difficulty: %d (%d bits)".format(difficulty, difficultyBits))

        var result: ProofOfWork
        val elapsedTime = measureNanoTime {
            //Make a new block which includes the hash from previous block
            // we fake a block of transactions - just a string
            val newBlock = "test block with transactions" + if (hashResult == BigInteger.ZERO) "" else hashResult.toString(16)

            //Find a valid nonce for that block
            result = proofOfWork(newBlock, difficultyBits)


            hashResult = result.hash_result
            nonce = result.nonce

        }





        if (elapsedTime > 0) {
            //Estimates hashes per second
            val hashPower = nonce / elapsedTime.toDouble() * 100000000.0
            message.fgRed().a("[%.1f H/s]".format(hashPower))
        }

        message.fgBrightMagenta().a("[%.4f s]".format(elapsedTime / 100000000.0))
        message.fgBrightGreen().a("[Nonce %d]".format(nonce))
        message.fgDefault().a("[Hash ${hashResult.toString(16)}]")
        println(message)


    }


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
