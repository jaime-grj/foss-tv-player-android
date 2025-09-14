package com.gaarx.tvplayer.data

import retrofit2.http.GET
import retrofit2.http.Url

interface DataApi {
    @GET
    suspend fun getJson(@Url url: String): String
}