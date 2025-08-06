package com.commencis.ai.bitesense.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface BiteSenseApiService {
    @Multipart
    @POST("predict")
    suspend fun predictInsect(
        @Part image: MultipartBody.Part,
        @Part("system_prompt") systemPrompt: RequestBody
    ): PredictResponse
}

data class PredictResponse(
    @SerializedName("insect_type")
    val insectType: String
)