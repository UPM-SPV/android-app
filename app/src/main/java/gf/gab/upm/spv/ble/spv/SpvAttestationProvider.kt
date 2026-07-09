package gf.gab.upm.spv.ble.spv

import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.config.SpvConfig
import gf.gab.upm.spv.util.logDebug
import gf.gab.upm.spv.util.logVerbose
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1String
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import java.math.BigInteger
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class SpvAttestationProvider<T>(private val endpoint: T)
        where T: ISpvAttestationEndpoint, T: ISpvDisEndpoint
{

    suspend fun attestDevice(timeoutMs: Long = 10_000) {
        logDebug("Starting attestation engine")
        val macAddress = endpoint.getMacAddress() ?:
            throw SecurityException("Unable to read MAC address")
        logVerbose("    MAC address:    $macAddress")

        val cert = endpoint.readCertificate()
            ?: throw SecurityException("Certificate read failed.")
        logVerbose("    Device certificate: ${cert.serialNumber}")

        val serialNumber = endpoint.readSerialNumber()
            ?: throw SecurityException("Serial number read failed.")
        logVerbose("    Device serial:  $serialNumber")

        val certFactory = CertificateFactory.getInstance("X.509")
        val caCert = certFactory
            .generateCertificate(SpvConfig.CA_CRT_PEM.encodeToByteArray().inputStream()) as X509Certificate

        logDebug("Verifying device certificate")
        cert.verify(caCert.publicKey)

        logDebug("Obtaining certificate extensions")
        val certSerialNumber = cert.getSubjectSerialNumber() ?:
        throw SecurityException("Certificate doesn't contain serial number")
        logVerbose("    Cert. device serial:    $serialNumber")
        val certMacAddress =  cert.getMacAddress() ?:
        throw SecurityException("Certificate doesn't contain MAC address")
        logVerbose("    Cert. device MAC:    $certMacAddress")

        if(certSerialNumber != serialNumber) {
            throw SecurityException("Certificate serial number mismatch")
        }

        if(certMacAddress != macAddress) {
            throw SecurityException("Certificate MAC address mismatch")
        }

        logDebug("Generating random attestation challenge")
        val random = SecureRandom()
        val nonce = ByteArray(SpvConfig.ATTEST_CHALLENGE_LENGTH)
        random.nextBytes(nonce)

        var response: ByteArray
        coroutineScope {
            logDebug("Sending challenge")
            endpoint.enableChallengeResponseIndication()
            val indicationDeferred = async {
                endpoint.waitForChallengeResponse(timeoutMs)
            }

            endpoint.writeChallenge(nonce)

            response = indicationDeferred.await()
                ?: throw SecurityException("No response to challenge")
        }

        logDebug("Verifying challenge response")
        val signature = reconstructSignature(response)
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(cert)
        sig.update(nonce)
        if(!sig.verify(signature)) {
            throw SecurityException("Invalid signature")
        }

        logDebug("Device attestation success for $macAddress")
        endpoint.markAuthenticated(true)
    }
}

private fun X509Certificate.getSubjectSerialNumber(): String? {
    val x500Name = JcaX509CertificateHolder(this).subject
    val serialNumber = x500Name.getRDNs(BCStyle.SERIALNUMBER)

    if(serialNumber.isEmpty()) {
        return null
    }

    return IETFUtils.valueToString(serialNumber.first().first.value)
}

private fun X509Certificate.getMacAddress(): MacAddress? {
    val MAC_ADDRESS_OID = "1.3.6.1.1.1.1.22"
    val macAsn = getExtensionValue(MAC_ADDRESS_OID) ?: return null

    val asnObject = ASN1InputStream(macAsn).readObject()
    if(asnObject !is ASN1OctetString) {
        return null
    }

    val objectBytes = asnObject.octets
    val innerObject = ASN1InputStream(objectBytes).readObject()
    if(innerObject !is ASN1String) {
        return null
    }

    return MacAddress.fromString(innerObject.toString())
}

private fun reconstructSignature(signature: ByteArray): ByteArray {
    if(signature.size != 64) {
        throw SecurityException("Malformed signature")
    }

    val r = BigInteger(1, signature.copyOfRange(0, 32))
    val s = BigInteger(1, signature.copyOfRange(32, 64))

    val vector = ASN1EncodableVector().apply {
        add(ASN1Integer(r))
        add(ASN1Integer(s))
    }

    return DERSequence(vector).encoded
}
