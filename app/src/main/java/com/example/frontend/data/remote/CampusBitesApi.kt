package com.example.frontend.data.remote

import com.example.frontend.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CampusBitesApi {
    @GET("api/v1/spots")
    suspend fun listarSitios(): List<SpotDto>

    @GET("api/v1/spots/{id}")
    suspend fun obtenerSitio(@Path("id") id: String): SpotDto

    @POST("api/v1/telemetry/page-loads")
    suspend fun enviarCargas(@Body batch: PageLoadBatchDto): IngestResultDto

    // BQ3 (tipo 4): "vistas de pagina + busquedas por restaurante en cada hora". La respuesta de la
    // business question llega directo a la UI como feature. `hour` = hora local del celular (contexto);
    // sin `hour` el backend devuelve todas las horas con actividad.
    @GET("api/v1/analytics/spot-views-by-hour")
    suspend fun vistasPorHora(
        @Query("hour") hour: Int?,
        @Query("tzOffsetMinutes") tzOffsetMinutes: Int,
        @Query("days") days: Int = 7,
        @Query("limit") limit: Int = 5,
    ): SpotViewsByHourDto
}

object ApiClient {
    // Timeout de 10 s: una carga que lo supere se reporta como falla TIMEOUT.
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    val api: CampusBitesApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CampusBitesApi::class.java)
}
