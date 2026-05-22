package au.com.harcourtapples.stocktake.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentUrl: String? = null
    private var currentKey: String? = null

    fun service(baseUrl: String, apiKey: String = ""): ApiService {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (retrofit == null || currentUrl != url || currentKey != apiKey) {
            val http = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val req = if (apiKey.isNotBlank()) {
                        chain.request().newBuilder()
                            .header("X-API-Key", apiKey)
                            .build()
                    } else {
                        chain.request()
                    }
                    chain.proceed(req)
                })
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentUrl = url
            currentKey = apiKey
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
