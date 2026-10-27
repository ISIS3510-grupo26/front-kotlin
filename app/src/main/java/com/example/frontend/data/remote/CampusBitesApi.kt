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

    // BQ4 (tipo 2): ultimo tiempo de espera reportado de cada restaurante.
    @GET("api/v1/spots/wait-times/latest")
    suspend fun ultimosTiemposEspera(): LastWaitTimesDto

    @POST("api/v1/spots/{id}/wait-times")
    suspend fun reportarEspera(@Path("id") id: String, @Body reporte: WaitTimeReportDto): WaitTimeReportResultDto

    // Autenticacion basica con JWT. El token lo agrega InterceptorSesion a cada peticion.
    @POST("api/v1/auth/login")
    suspend fun iniciarSesion(@Body credenciales: CredencialesDto): AuthTokenDto

    @POST("api/v1/auth/signup")
    suspend fun registrarse(@Body credenciales: CredencialesDto): AuthTokenDto

    @GET("api/v1/auth/me")
    suspend fun yo(): UsuarioDto

    // BQ3 (tipo 2): los mejor calificados cerca de (lat, lng) que el usuario todavia no ha probado.
    @GET("api/v1/users/{userId}/recommendations/nearby")
    suspend fun recomendacionesCercanas(
        @Path("userId") userId: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("maxWalkMinutes") maxWalkMinutes: Int = 15,
        @Query("limit") limit: Int = 5,
    ): List<NearbyPickDto>

    @GET("api/v1/analytics/searches-by-weekday")
    suspend fun busquedasPorDia(
        @Query("tzOffsetMinutes") tzOffsetMinutes: Int,
        @Query("days") days: Int = 28,
    ): SearchesByWeekdayDto
}

object ApiClient {
    // Timeout de 10 s: una carga que lo supere se reporta como falla TIMEOUT.
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(InterceptorSesion)
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
