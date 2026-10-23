package com.prajanestham.shortnews

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WpRendered(val rendered: String?)
data class WpMediaDetails(val source_url: String?)
data class WpEmbeddedMedia(val source_url: String?, val media_details: WpMediaDetails?)
data class WpPost(
    val id: Long,
    val date: String?,
    val link: String?,
    val title: WpRendered?,
    val excerpt: WpRendered?,
    val content: WpRendered?,
    val featured_media: Long?,
    val categories: List<Int>?,
    val _embedded: Map<String, Any>?
) {
    // _embed response is parsed with Gson's generic map; featured image is extracted separately below.
    var imageUrl: String? = null
    var district: String = "తెలంగాణ"
}

interface WpApi {
    @GET("wp-json/wp/v2/posts?_embed=1")
    suspend fun posts(
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("categories") category: Int? = null,
        @Query("status") status: String? = null
    ): List<WpPost>
}

object ApiClient {
    // Keep the existing WordPress site unchanged. Set SHORT_NEWS_CATEGORY_ID after creating/identifying the category.
    const val BASE_URL = "https://www.prajanestham.com/"
    const val SHORT_NEWS_CATEGORY_ID = 0 // TODO: replace with the actual WordPress short-news category ID.
    val api: WpApi by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(WpApi::class.java)
    }
}
