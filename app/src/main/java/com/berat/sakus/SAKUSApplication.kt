package com.berat.sakus

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.berat.sakus.data.notification.NewsNotificationHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SAKUSApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        NewsNotificationHelper.ensureChannel(this)
        com.berat.sakus.data.notification.LineUpdateNotificationHelper.ensureChannel(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "SAKUS/1.0 (Android; Toplu Tasima)")
                    .addHeader("Accept", "image/*")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
