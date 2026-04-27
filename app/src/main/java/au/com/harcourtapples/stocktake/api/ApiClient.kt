package au.com.harcourtapples.stocktake.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentUrl: String? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun service(baseUrl: String): ApiService {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (retrofit == null || currentUrl != url) {
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentUrl = url
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
