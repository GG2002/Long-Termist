package com.cyc.yearlymemoir.data.tidbcloud.db

import com.cyc.yearlymemoir.data.tidbcloud.dao.DetailDao
import com.cyc.yearlymemoir.data.tidbcloud.dao.FieldDao
import com.cyc.yearlymemoir.data.tidbcloud.dao.GroupDao
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class DbClient(
    private val publicKey: String = "82B3MH50",
    private val privateKey: String = "830a97fa-2642-48d8-a876-ae2d923c2aeb",
) {
    val detailDao by lazy { DetailDao(this) }
    val fieldDao by lazy { FieldDao(this) }
    val groupDao by lazy { GroupDao(this) }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .authenticator(object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                if (response.request.header("Authorization") != null) {
                    return null // 如果已经尝试过授权，则放弃
                }
                val credential = Credentials.basic(publicKey, privateKey)
                return response.request.newBuilder()
                    .header("Authorization", credential)
                    .build()
            }
        })
        .build()

    private val baseUrl =
        "https://eu-central-1.data.tidbcloud.com/api/v1beta/app/dataapp-ohKLfneI/endpoint/"

    fun <TRequest, TResponse> post(
        endpoint: String,
        requestBody: TRequest,
        requestSerializer: KSerializer<TRequest>,
        responseSerializer: KSerializer<TResponse>
    ): TResponse {
        // 将请求体对象序列化为 JSON 字符串
        val jsonBody = json.encodeToString(requestSerializer, requestBody)

        // 创建请求
        val request = Request.Builder()
            .url(baseUrl + endpoint)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .header("endpoint-type", "draft") // 添加固定的请求头
            .build()

        // 执行请求并处理响应
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // 如果请求失败，抛出异常
                throw IOException("API Error: ${response.code} ${response.message} - ${response.body?.string()}")
            }
            val responseBodyString = response.body?.string()
                ?: throw IOException("Empty response body")

            // 将响应的 JSON 字符串反序列化为指定的响应对象
            return json.decodeFromString(responseSerializer, responseBodyString)
        }
    }
}

@Serializable
data class ApiResponse<T>(
    val type: String,
    val data: tableData<T>? = null
)

@Serializable
data class tableData<T>(
    val columns: List<colInfo>,
    val result: TidbResult,
    val rows: List<T>
)

@Serializable
data class colInfo(
    val col: String,
    val data_type: String,
    val nullable: Boolean
)

@Serializable
data class TidbResult(
    val code: Int,
    val start_ms: Long,
    val end_ms: Long,
    val latency: String,
    val limit: Int,
    val message: String,
    val row_affect: Int,
    val row_count: Int,
)