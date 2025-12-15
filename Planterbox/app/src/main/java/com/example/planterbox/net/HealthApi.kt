package com.example.planterbox.net

import retrofit2.http.GET

data class HealthResponse(val status: String?, val server: String?)

interface HealthApi {
    @GET("health")
    suspend fun health(): HealthResponse
}
