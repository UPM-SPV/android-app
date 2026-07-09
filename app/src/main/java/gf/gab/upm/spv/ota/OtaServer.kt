package gf.gab.upm.spv.ota

import android.os.Parcelable
import gf.gab.upm.spv.util.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.source
import org.json.JSONArray
import javax.inject.Inject
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OtaServer {
    @Headers(
        "User-Agent: UpmSpv",
        "Accept: application/json",
        "X-GitHub-Api-Version: 2026-03-10",
    )
    @GET("releases?page=1&per_page=1")
    suspend fun getReleases(): ResponseBody

    @Streaming
    @Headers(
        "User-Agent: UpmSpv",
        "Accept: application/octet-stream",
        "X-GitHub-Api-Version: 2026-03-10",
    )
    @GET
    suspend fun downloadAsset(@Url assetUrl: String): ResponseBody
}

class OtaServerManager @Inject constructor(
    private val otaServer: OtaServer
) {
    suspend fun getLatestRelease(): OtaInfo? {
        val rawResponse = try {
            otaServer.getReleases()
        } catch(e: Exception) {
            logError("Error fetching latest OTA update: $e")
            return null
        }

        val releaseList = JSONArray(rawResponse.string())

        val latestRelease = releaseList.optJSONObject(0) ?: return null
        val name = latestRelease.optString("name") ?: return null
        val assetList = latestRelease.optJSONArray("assets") ?: return null
        val dfuAsset = assetList.optJSONObject(0) ?: return null
        val dfuUrl = dfuAsset.optString("url") ?: return null
        val dfuSize = dfuAsset.optLong("size").let { if(it == 0L) null else it } ?: return null

        return OtaInfo(
            version = OtaVersion.tryParseSemver(name) ?: return null,
            url = dfuUrl,
            sizeBytes = dfuSize
        )
    }

    suspend fun downloadDfu(info: OtaInfo, progressCallback: ((Long) -> Unit)? = null): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                otaServer.downloadAsset(info.url).use { responseBody ->
                    val totalBytes = responseBody.contentLength()
                    if(totalBytes != info.sizeBytes) {
                        return@use null
                    }

                    val inputStream = responseBody.byteStream()

                    inputStream.source().use { source ->
                        val buffer = Buffer()
                        var totalBytesRead = 0L
                        val readSize = 4096L

                        while(true) {
                            val bytesRead = source.read(buffer, readSize)
                            if(bytesRead == -1L) break

                            totalBytesRead += bytesRead
                            progressCallback?.invoke(totalBytesRead)
                        }

                        progressCallback?.invoke(totalBytes)

                        buffer.readByteArray()
                    }
                }
            } catch(e: Exception) {
                logError("Error downloading OTA update: $e")
                null
            }
        }
    }
}

@Serializable
@Parcelize
data class OtaInfo(
    val version: OtaVersion,
    val url: String,
    val sizeBytes: Long,
) : Parcelable

@Serializable
@Parcelize
data class OtaVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<OtaVersion>, Parcelable {

    companion object {
        fun tryParseSemver(version: String): OtaVersion? {
            val regex = Regex(".*([0-9]+)\\.([0-9]+)\\.([0-9]+).*").find(version) ?: return null
            val (major, minor, patch) = regex.destructured

            return OtaVersion(major.toInt(), minor.toInt(), patch.toInt())
        }

    }

    override fun toString(): String = "${major}.${minor}.${patch}"

    override fun compareTo(other: OtaVersion): Int =
        when(val result = major.compareTo(other.major)) {
            0 -> when(val result = minor.compareTo(other.minor)) {
                0 -> patch.compareTo(other.patch)
                else -> return result
            }
            else -> return result
        }
}
