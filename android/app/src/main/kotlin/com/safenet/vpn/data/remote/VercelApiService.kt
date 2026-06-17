package com.safenet.vpn.data.remote

import retrofit2.Response
import retrofit2.http.GET

data class VercelServersResponse(
    val success: Boolean,
    val servers: List<VercelServer>
)

data class VercelServer(
    val id: String,
    val name: String,
    val protocol: String,
    val config: String
)

interface VercelApiService {
    @GET("servers")
    suspend fun getServers(): Response<VercelServersResponse>
}
