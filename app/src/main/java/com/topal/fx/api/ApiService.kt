package com.topal.fx.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service representing Frankfurter API endpoints.
 */
interface ApiService {
    /**
     * Fetches the latest exchange rate.
     * Default from is EUR, default to is USD.
     */
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") from: String = "EUR",
        @Query("to") to: String = "USD"
    ): FrankfurterResponse
}
