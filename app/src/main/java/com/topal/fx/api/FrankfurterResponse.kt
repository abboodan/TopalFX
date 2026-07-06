package com.topal.fx.api

import com.google.gson.annotations.SerializedName

/**
 * Data model for the Frankfurter API response.
 * Example structure:
 * {
 *   "amount": 1.0,
 *   "base": "EUR",
 *   "date": "2026-06-26",
 *   "rates": { "USD": 1.0765 }
 * }
 */
data class FrankfurterResponse(
    @SerializedName("amount") val amount: Double,
    @SerializedName("base") val base: String,
    @SerializedName("date") val date: String,
    @SerializedName("rates") val rates: Map<String, Double>
)
