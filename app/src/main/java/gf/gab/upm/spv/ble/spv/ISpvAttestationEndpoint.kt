package gf.gab.upm.spv.ble.spv

import gf.gab.upm.spv.ble.MacAddress
import java.security.cert.X509Certificate

interface ISpvAttestationEndpoint {
    suspend fun readCertificate(): X509Certificate?
    suspend fun enableChallengeResponseIndication()
    suspend fun waitForChallengeResponse(timeoutMs: Long): ByteArray?
    suspend fun writeChallenge(challenge: ByteArray)
    fun markAuthenticated(isAuthenticated: Boolean)
}
