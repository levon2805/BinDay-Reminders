package com.example.binminder.data.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Data transfer object holding administrative boundary details returned from UK postcode lookup services.
 */
data class PostcodeLookupDto(
    val postcode: String,
    val adminDistrict: String,
    val parish: String? = null,
    val region: String? = null,
    val outcode: String? = null,
    val incode: String? = null
)

/**
 * Service interface for querying UK postcode administrative data.
 */
interface CouncilLookupService {
    /**
     * Fetches administrative details for a given UK postcode string.
     */
    suspend fun lookupPostcode(postcode: String): PostcodeLookupDto?
}

/**
 * Service implementation querying the postcodes.io public API for UK administrative council data.
 */
class CouncilLookupServiceImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : CouncilLookupService {

    /**
     * Connects to the postcodes.io API to resolve administrative council details for the specified postcode.
     */
    override suspend fun lookupPostcode(postcode: String): PostcodeLookupDto? = withContext(Dispatchers.IO) {
        val sanitizedPostcode = postcode.replace("\\s+".toRegex(), "").trim().uppercase()
        if (sanitizedPostcode.isBlank()) return@withContext null

        try {
            val encodedPostcode = URLEncoder.encode(sanitizedPostcode, "UTF-8")
            val request = Request.Builder()
                .url("https://api.postcodes.io/postcodes/$encodedPostcode")
                .header("User-Agent", "BinMinder-AndroidApp/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null

                val json = JSONObject(bodyString)
                val status = json.optInt("status", 0)
                if (status != 200) return@withContext null

                val resultObj = json.optJSONObject("result") ?: return@withContext null

                val formattedPostcode = resultObj.optString("postcode", sanitizedPostcode)
                val adminDistrict = resultObj.optString("admin_district", "")
                val parish = if (resultObj.isNull("parish")) null else resultObj.optString("parish")
                val region = if (resultObj.isNull("region")) null else resultObj.optString("region")
                val outcode = if (resultObj.isNull("outcode")) null else resultObj.optString("outcode")
                val incode = if (resultObj.isNull("incode")) null else resultObj.optString("incode")

                if (adminDistrict.isBlank()) return@withContext null

                PostcodeLookupDto(
                    postcode = formattedPostcode,
                    adminDistrict = adminDistrict,
                    parish = parish,
                    region = region,
                    outcode = outcode,
                    incode = incode
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
