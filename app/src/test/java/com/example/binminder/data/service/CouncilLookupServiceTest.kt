package com.example.binminder.data.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CouncilLookupServiceTest {

    private fun createServiceWithInterceptor(interceptor: Interceptor): CouncilLookupServiceImpl {
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        return CouncilLookupServiceImpl(client)
    }

    @Test
    fun testLookupPostcodeSuccessfulResponse() = runTest {
        val jsonBody = """
            {
                "status": 200,
                "result": {
                    "postcode": "SW1A 1AA",
                    "admin_district": "Westminster",
                    "region": "London"
                }
            }
        """.trimIndent()

        val service = createServiceWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonBody.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = service.lookupPostcode("SW1A 1AA")
        assertEquals("SW1A 1AA", result?.postcode)
        assertEquals("Westminster", result?.adminDistrict)
        assertEquals("London", result?.region)
    }

    @Test
    fun testLookupPostcodeHttp404NotFoundReturnsNull() = runTest {
        val service = createServiceWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = service.lookupPostcode("INVALID")
        assertNull(result)
    }

    @Test
    fun testLookupPostcodeHttp500ServerErrorReturnsNull() = runTest {
        val service = createServiceWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Server Error")
                .body("Internal Server Error".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val result = service.lookupPostcode("SW1A 1AA")
        assertNull(result)
    }

    @Test
    fun testLookupPostcodeMalformedJsonReturnsNull() = runTest {
        val malformedJson = "{ status: 200, result: invalid json... "

        val service = createServiceWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(malformedJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = service.lookupPostcode("SW1A 1AA")
        assertNull(result)
    }

    @Test
    fun testLookupPostcodeEmptyResponseBodyReturnsNull() = runTest {
        val service = createServiceWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = service.lookupPostcode("SW1A 1AA")
        assertNull(result)
    }

    @Test
    fun testLookupPostcodeNetworkIOExceptionReturnsNull() = runTest {
        val service = createServiceWithInterceptor { _ ->
            throw IOException("Socket timeout / Connection reset")
        }

        val result = service.lookupPostcode("SW1A 1AA")
        assertNull(result)
    }

    @Test
    fun testLookupPostcodeBlankPostcodeReturnsNull() = runTest {
        val service = CouncilLookupServiceImpl()
        val result = service.lookupPostcode("   ")
        assertNull(result)
    }
}
