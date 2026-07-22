package com.topal.fx

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topal.fx.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Supported Transfer Directions.
 */
enum class TransferDirection {
    EUR_TO_USD, USD_TO_EUR
}

/**
 * Supported Transfer Input Modes.
 */
enum class TransferMode {
    SEND_BASE,      // Mode A: Customer provides exact base amount
    RECEIVE_TARGET, // Mode B: Customer wants receiver to get exact target amount
    CUSTOM_DEAL     // Mode C: Custom Deal (Flat Exchange)
}

/**
 * Representation of the calculator results.
 */
data class CalculationResults(
    val totalAmountPaidBase: Double,
    val totalAmountReceivedTarget: Double,
    val baseAmount: Double,
    val flatFeeBase: Double,
    val pctFeeBase: Double,
    val deliveryFeeTarget: Double,
    val grossFeeProfitBase: Double,
    val hiddenSpreadProfitBase: Double,
    val flatAgentCostBase: Double,
    val pctAgentCostBase: Double,
    val totalAgentCostBase: Double,
    val netProfitBase: Double
)

/**
 * Data class representing a currency exchange rate in the watchlist.
 */
data class CurrencyRate(
    val baseCurrencyFlagUrl: String,
    val symbolPair: String,
    val fullNamePair: String,
    val currentRate: Double,
    val changePercentage: Double,
    val trendPoints: List<Double>
)

/**
 * Supported UI languages.
 */
enum class Language {
    EN, AR
}

/**
 * Types of calculation validation errors.
 */
enum class CalculationError {
    NONE, INVALID_BASE, INVALID_RATE, FEE_EXCEED
}

/**
 * Supported Deduction Base Options for Office Cost.
 */
enum class DeductionBase {
    ON_RECEIVED_BASE,   // Option A: On Received Amount (EUR)
    ON_DELIVERED_TARGET // Option B: On Delivered Target (USD)
}

/**
 * Representation of the UI state for the remittance calculator.
 */
data class CalculatorUiState(
    val amountInput: String = "", // Send or Receive exact amount depending on Mode
    val customDealTargetAmount: String = "", // Used in Mode C
    
    // Fee inputs
    val flatFee: String = "",
    val pctFee: String = "",
    val deliveryFee: String = "",
    
    // Exchange Rates
    val marketRate: String = "",
    val customerRate: String = "",
    val isRateManuallyEdited: Boolean = false,
    
    // Direction & Mode settings
    val transferDirection: TransferDirection = TransferDirection.EUR_TO_USD,
    val transferMode: TransferMode = TransferMode.SEND_BASE,
    val deductionBase: DeductionBase = DeductionBase.ON_RECEIVED_BASE,
    
    // Admin Cost inputs
    val flatAgentCost: String = "",
    val pctAgentCost: String = "",
    val isAdminPanelExpanded: Boolean = false,
    
    // Language & Status flags
    val language: Language = Language.AR, // Default to Arabic
    val isFetchingRate: Boolean = false,
    val hasRateFetchError: Boolean = false,
    val calculationResults: CalculationResults? = null,
    val calculationError: CalculationError = CalculationError.NONE,
    
    val tickerRates: List<CurrencyRate> = emptyList(),
    val isFetchingTicker: Boolean = false,
    val isFeeInclusive: Boolean = false
)

/**
 * ViewModel managing the state, auto-fetching, persistence, and bi-directional calculations.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("topalfx_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var autoUpdateJob: Job? = null
    
    // Keeps track of previous rates to calculate trend indicators
    private val previousRatesMap = mutableMapOf<String, Double>()
    private val trendPointsMap = mutableMapOf<String, MutableList<Double>>()

    init {
        loadDefaultSettings()
        fetchExchangeRateDirect(silent = false)
        fetchTickerRates()
        startAutoUpdateTicker()
    }

    /**
     * Reads saved settings from SharedPreferences and fills UI inputs.
     */
    private fun loadDefaultSettings() {
        val defaultCustomerFee = sharedPrefs.getString("default_customer_fee", "") ?: ""
        val defaultDeliveryFee = sharedPrefs.getString("default_delivery_fee", "") ?: ""
        val defaultOfficePercentage = sharedPrefs.getString("default_office_percentage", "") ?: ""
        
        _uiState.update {
            it.copy(
                pctFee = defaultCustomerFee,
                deliveryFee = defaultDeliveryFee,
                pctAgentCost = defaultOfficePercentage
            )
        }
    }

    /**
     * Saves the default configuration to SharedPreferences and applies it.
     */
    fun saveDefaultSettings(pctFee: String, deliveryFee: String, pctAgentCost: String) {
        sharedPrefs.edit()
            .putString("default_customer_fee", pctFee)
            .putString("default_delivery_fee", deliveryFee)
            .putString("default_office_percentage", pctAgentCost)
            .apply()
        
        _uiState.update {
            it.copy(
                pctFee = pctFee,
                deliveryFee = deliveryFee,
                pctAgentCost = pctAgentCost,
                calculationError = CalculationError.NONE
            )
        }
    }

    /**
     * Reads the current default configuration from SharedPreferences.
     */
    fun getDefaultSettings(): Triple<String, String, String> {
        val defaultCustomerFee = sharedPrefs.getString("default_customer_fee", "") ?: ""
        val defaultDeliveryFee = sharedPrefs.getString("default_delivery_fee", "") ?: ""
        val defaultOfficePercentage = sharedPrefs.getString("default_office_percentage", "") ?: ""
        return Triple(defaultCustomerFee, defaultDeliveryFee, defaultOfficePercentage)
    }

    /**
     * Toggles the UI language between English and Arabic.
     */
    fun toggleLanguage() {
        _uiState.update { state ->
            val nextLang = if (state.language == Language.EN) Language.AR else Language.EN
            val updatedTickers = state.tickerRates.map { rate ->
                val parts = rate.symbolPair.split("/")
                val base = parts.getOrNull(0) ?: ""
                val quote = parts.getOrNull(1) ?: ""
                val baseName = getCurrencyName(base, nextLang)
                val quoteName = getCurrencyName(quote, nextLang)
                rate.copy(fullNamePair = "$baseName / $quoteName")
            }
            state.copy(
                language = nextLang,
                tickerRates = updatedTickers
            )
        }
    }

    /**
     * Initiates a manual update of the rate for the calculator.
     */
    fun fetchExchangeRate() {
        _uiState.update { it.copy(isRateManuallyEdited = false) }
        fetchExchangeRateDirect(silent = false)
    }

    /**
     * Starts the auto-update ticker loop running every 5 seconds.
     */
    private fun startAutoUpdateTicker() {
        autoUpdateJob?.cancel()
        autoUpdateJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                // 1. Update calculator rate in background if not overridden
                if (!_uiState.value.isRateManuallyEdited) {
                    fetchExchangeRateDirect(silent = true)
                }
                // 2. Update ticker board rates
                fetchTickerRatesSilent()
            }
        }
    }

    /**
     * Fetches rate from Frankfurter API based on selected transfer direction.
     */
    private fun fetchExchangeRateDirect(silent: Boolean) {
        val direction = _uiState.value.transferDirection
        val from = if (direction == TransferDirection.EUR_TO_USD) "EUR" else "USD"
        val to = if (direction == TransferDirection.EUR_TO_USD) "USD" else "EUR"

        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isFetchingRate = true, hasRateFetchError = false) }
            }
            try {
                val response = RetrofitClient.apiService.getLatestRates(from = from, to = to)
                val rate = response.rates[to]
                if (rate != null) {
                    _uiState.update { 
                        if (it.transferDirection == direction && !it.isRateManuallyEdited) {
                            it.copy(
                                marketRate = String.format(Locale.US, "%.4f", rate),
                                customerRate = String.format(Locale.US, "%.4f", rate),
                                isFetchingRate = false,
                                hasRateFetchError = false
                            )
                        } else {
                            it.copy(isFetchingRate = false)
                        }
                    }
                } else {
                    if (!silent) {
                        _uiState.update { it.copy(isFetchingRate = false, hasRateFetchError = true) }
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update { it.copy(isFetchingRate = false, hasRateFetchError = true) }
                }
            }
        }
    }

    /**
     * Fetches the exchange rates for all ticker pairs.
     */
    fun fetchTickerRates() {
        _uiState.update { it.copy(isFetchingTicker = true) }
        fetchTickerRatesSilent()
    }

    /**
     * Background ticker fetcher.
     */
    private fun fetchTickerRatesSilent() {
        viewModelScope.launch {
            val pairsStr = sharedPrefs.getString("ticker_pairs", "EUR/USD;EUR/TRY;USD/TRY") ?: "EUR/USD;EUR/TRY;USD/TRY"
            val pairs = parsePairs(pairsStr)
            
            val deferredRates = pairs.map { pair ->
                async {
                    val key = "${pair.first}/${pair.second}"
                    val lang = _uiState.value.language
                    val baseName = getCurrencyName(pair.first, lang)
                    val quoteName = getCurrencyName(pair.second, lang)
                    val fullName = "$baseName / $quoteName"
                    try {
                        val response = RetrofitClient.apiService.getLatestRates(from = pair.first, to = pair.second)
                        val rate = response.rates[pair.second]
                        if (rate != null) {
                            val rateVal: Double = rate
                            val prevRate = previousRatesMap[key] ?: 0.0
                            val pctChange = if (prevRate > 0.0) {
                                ((rateVal - prevRate) / prevRate) * 100.0
                            } else 0.0
                            previousRatesMap[key] = rateVal
                            
                            // Get/generate trend points
                            val points = trendPointsMap.getOrPut(key) {
                                val list = mutableListOf<Double>()
                                val random = java.util.Random()
                                for (i in 0 until 9) {
                                    val offset: Double = (random.nextDouble() - 0.5) * 0.005 * rateVal
                                    list.add(rateVal + offset)
                                }
                                list.add(rateVal)
                                list
                            }
                            if (points.isEmpty() || points.last() != rate) {
                                points.add(rate)
                                if (points.size > 15) {
                                    points.removeAt(0)
                                }
                            }
                            
                            CurrencyRate(
                                baseCurrencyFlagUrl = getFlagUrl(pair.first),
                                symbolPair = key,
                                fullNamePair = fullName,
                                currentRate = rate,
                                changePercentage = pctChange,
                                trendPoints = points.toList()
                            )
                        } else {
                            CurrencyRate(
                                baseCurrencyFlagUrl = getFlagUrl(pair.first),
                                symbolPair = key,
                                fullNamePair = fullName,
                                currentRate = 0.0,
                                changePercentage = 0.0,
                                trendPoints = trendPointsMap[key]?.toList() ?: emptyList()
                            )
                        }
                    } catch (e: Exception) {
                        CurrencyRate(
                            baseCurrencyFlagUrl = getFlagUrl(pair.first),
                            symbolPair = key,
                            fullNamePair = fullName,
                            currentRate = 0.0,
                            changePercentage = 0.0,
                            trendPoints = trendPointsMap[key]?.toList() ?: emptyList()
                        )
                    }
                }
            }
            
            val results = deferredRates.awaitAll().filterNotNull()
            _uiState.update {
                it.copy(
                    tickerRates = results,
                    isFetchingTicker = false
                )
            }
        }
    }

    private fun parsePairs(serialized: String): List<Pair<String, String>> {
        return serialized.split(";").mapNotNull {
            val parts = it.split("/")
            if (parts.size == 2) Pair(parts[0].trim().uppercase(), parts[1].trim().uppercase()) else null
        }
    }

    /**
     * Adds a currency pair to the Live Rates Board.
     */
    fun addTickerPair(from: String, to: String) {
        val cleanFrom = from.trim().uppercase()
        val cleanTo = to.trim().uppercase()
        if (cleanFrom.isEmpty() || cleanTo.isEmpty() || cleanFrom == cleanTo) return

        val pairsStr = sharedPrefs.getString("ticker_pairs", "EUR/USD;EUR/TRY;USD/TRY") ?: "EUR/USD;EUR/TRY;USD/TRY"
        val pairs = parsePairs(pairsStr).toMutableList()
        val newPair = Pair(cleanFrom, cleanTo)
        
        if (!pairs.contains(newPair)) {
            pairs.add(newPair)
            val newSerialized = pairs.joinToString(";") { "${it.first}/${it.second}" }
            sharedPrefs.edit().putString("ticker_pairs", newSerialized).apply()
            fetchTickerRates()
        }
    }

    /**
     * Removes a currency pair from the Live Rates Board.
     */
    fun deleteTickerPair(pair: CurrencyRate) {
        val pairsStr = sharedPrefs.getString("ticker_pairs", "EUR/USD;EUR/TRY;USD/TRY") ?: "EUR/USD;EUR/TRY;USD/TRY"
        val pairs = parsePairs(pairsStr).toMutableList()
        val parts = pair.symbolPair.split("/")
        if (parts.size == 2) {
            val target = Pair(parts[0], parts[1])
            if (pairs.remove(target)) {
                val newSerialized = pairs.joinToString(";") { "${it.first}/${it.second}" }
                sharedPrefs.edit().putString("ticker_pairs", newSerialized).apply()
                
                // Remove from cache
                val key = "${parts[0]}/${parts[1]}"
                previousRatesMap.remove(key)
                trendPointsMap.remove(key)
                
                fetchTickerRates()
            }
        }
    }

    /**
     * Reads list of saved ticker pairs for settings manager.
     */
    fun getSavedTickerPairs(): List<Pair<String, String>> {
        val pairsStr = sharedPrefs.getString("ticker_pairs", "EUR/USD;EUR/TRY;USD/TRY") ?: "EUR/USD;EUR/TRY;USD/TRY"
        return parsePairs(pairsStr)
    }

    /**
     * Copies a clicked ticker rate to the calculator input and marks it as manually overridden.
     */
    fun selectRateFromTicker(rate: Double) {
        _uiState.update {
            it.copy(
                customerRate = String.format(Locale.US, "%.4f", rate),
                isRateManuallyEdited = true,
                calculationError = CalculationError.NONE
            )
        }
    }

    fun onAmountInputChanged(value: String) {
        _uiState.update { it.copy(amountInput = value, calculationError = CalculationError.NONE) }
    }

    fun onCustomDealTargetAmountChanged(value: String) {
        _uiState.update { it.copy(customDealTargetAmount = value, calculationError = CalculationError.NONE) }
    }

    fun onFlatFeeChanged(value: String) {
        _uiState.update { it.copy(flatFee = value, calculationError = CalculationError.NONE) }
    }

    fun onPctFeeChanged(value: String) {
        _uiState.update { it.copy(pctFee = value, calculationError = CalculationError.NONE) }
    }

    fun onDeliveryFeeChanged(value: String) {
        _uiState.update { it.copy(deliveryFee = value, calculationError = CalculationError.NONE) }
    }

    fun onCustomerRateChanged(value: String) {
        _uiState.update { 
            it.copy(
                customerRate = value, 
                isRateManuallyEdited = true, 
                calculationError = CalculationError.NONE 
            ) 
        }
    }

    fun onMarketRateChanged(value: String) {
        _uiState.update { 
            it.copy(
                marketRate = value, 
                isRateManuallyEdited = true, 
                calculationError = CalculationError.NONE 
            ) 
        }
    }

    fun clearInputs() {
        _uiState.update { 
            it.copy(
                amountInput = "",
                customDealTargetAmount = "",
                calculationResults = null,
                calculationError = CalculationError.NONE
            )
        }
    }

    fun onFeeInclusiveChanged(inclusive: Boolean) {
        _uiState.update { 
            it.copy(
                isFeeInclusive = inclusive,
                calculationResults = null,
                calculationError = CalculationError.NONE
            )
        }
    }

    fun onDeductionBaseChanged(base: DeductionBase) {
        _uiState.update {
            it.copy(
                deductionBase = base,
                calculationResults = null,
                calculationError = CalculationError.NONE
            )
        }
    }

    fun onFlatAgentCostChanged(value: String) {
        _uiState.update { it.copy(flatAgentCost = value, calculationError = CalculationError.NONE) }
    }

    fun onPctAgentCostChanged(value: String) {
        _uiState.update { it.copy(pctAgentCost = value, calculationError = CalculationError.NONE) }
    }

    fun toggleAdminPanel() {
        _uiState.update { it.copy(isAdminPanelExpanded = !it.isAdminPanelExpanded) }
    }

    fun onTransferDirectionChanged(direction: TransferDirection) {
        _uiState.update {
            it.copy(
                transferDirection = direction,
                isRateManuallyEdited = false,
                marketRate = "",
                customerRate = "",
                calculationResults = null,
                calculationError = CalculationError.NONE
            )
        }
        fetchExchangeRateDirect(silent = false)
    }

    fun onTransferModeChanged(mode: TransferMode) {
        _uiState.update {
            it.copy(
                transferMode = mode,
                calculationResults = null,
                calculationError = CalculationError.NONE
            )
        }
    }

    /**
     * Helper functions for calculations to enable easy unit-testing.
     */
    companion object {
        fun calculateSendBase(
            baseAmount: Double,
            marketRate: Double,
            customerRate: Double,
            flatFee: Double,
            pctFeePercent: Double,
            deliveryFee: Double,
            flatAgentCost: Double,
            pctAgentCostPercent: Double,
            direction: TransferDirection,
            isFeeInclusive: Boolean = false,
            deductionBase: DeductionBase = DeductionBase.ON_RECEIVED_BASE
        ): CalculationResults {
            // If fee is inclusive, we need to subtract the fees from baseAmount (which is the total amount paid)
            val principal = if (isFeeInclusive) {
                val derived = (baseAmount - flatFee) / (1.0 + pctFeePercent / 100.0)
                derived.coerceAtLeast(0.0)
            } else {
                baseAmount
            }

            // Convert principal amount to target amount at customer rate
            val targetAmountCust = principal * customerRate
            val targetAmountMkt = principal * marketRate

            // Receiver gets target amount minus delivery fee
            val totalAmountReceivedTarget = (targetAmountCust - deliveryFee).coerceAtLeast(0.0)

            // Percentage fee is calculated on the EUR amount (whether base or target)
            val pctFeeBase = if (direction == TransferDirection.EUR_TO_USD) {
                principal * pctFeePercent / 100.0
            } else {
                val eurAmount = targetAmountCust
                val pctFeeEur = eurAmount * pctFeePercent / 100.0
                pctFeeEur / customerRate
            }

            val totalAmountPaidBase = if (isFeeInclusive) baseAmount else principal + flatFee + pctFeeBase

            // Hidden spread profit = Base Amount * (Market Rate - Customer Rate) / Market Rate
            val spreadProfitTarget = targetAmountMkt - targetAmountCust
            val hiddenSpreadProfitBase = spreadProfitTarget / marketRate

            val grossFeeProfitBase = flatFee + pctFeeBase

            // Agent Costs calculated based on selected Deduction Base and rounded to nearest whole number
            val pctAgentCostBase = if (deductionBase == DeductionBase.ON_DELIVERED_TARGET) {
                val trueCostInEur = if (direction == TransferDirection.EUR_TO_USD) {
                    targetAmountCust / marketRate
                } else {
                    principal
                }
                kotlin.math.round(trueCostInEur * (pctAgentCostPercent / 100.0))
            } else {
                if (direction == TransferDirection.EUR_TO_USD) {
                    kotlin.math.round(baseAmount * (pctAgentCostPercent / 100.0))
                } else {
                    val eurAmount = targetAmountCust
                    val pctAgentCostEur = eurAmount * pctAgentCostPercent / 100.0
                    kotlin.math.round(pctAgentCostEur / customerRate)
                }
            }

            val deliveryFeeBase = if (direction == TransferDirection.EUR_TO_USD) deliveryFee / marketRate else deliveryFee
            val totalAgentCostBase = flatAgentCost + pctAgentCostBase
            val totalProfitBase = grossFeeProfitBase + hiddenSpreadProfitBase
            val netProfitBase = totalProfitBase - totalAgentCostBase - deliveryFeeBase

            return CalculationResults(
                totalAmountPaidBase = totalAmountPaidBase,
                totalAmountReceivedTarget = totalAmountReceivedTarget,
                baseAmount = principal,
                flatFeeBase = flatFee,
                pctFeeBase = pctFeeBase,
                deliveryFeeTarget = deliveryFee,
                grossFeeProfitBase = grossFeeProfitBase,
                hiddenSpreadProfitBase = hiddenSpreadProfitBase,
                flatAgentCostBase = flatAgentCost,
                pctAgentCostBase = pctAgentCostBase,
                totalAgentCostBase = totalAgentCostBase,
                netProfitBase = netProfitBase
            )
        }

        fun calculateReceiveTarget(
            targetAmount: Double,
            marketRate: Double,
            customerRate: Double,
            flatFee: Double,
            pctFeePercent: Double,
            deliveryFee: Double,
            flatAgentCost: Double,
            pctAgentCostPercent: Double,
            direction: TransferDirection,
            deductionBase: DeductionBase = DeductionBase.ON_RECEIVED_BASE
        ): CalculationResults {
            // Target amount needed before delivery fee is deducted
            val targetAmountCust = targetAmount + deliveryFee

            // Base amount needed at Customer Rate
            val baseAmount = targetAmountCust / customerRate

            // Base amount needed at Market Rate
            val baseAmountMkt = targetAmountCust / marketRate

            // Percentage fee is calculated on the EUR amount
            val pctFeeBase = if (direction == TransferDirection.EUR_TO_USD) {
                baseAmount * pctFeePercent / 100.0
            } else {
                val eurAmount = targetAmountCust
                val pctFeeEur = eurAmount * pctFeePercent / 100.0
                pctFeeEur / customerRate
            }

            val totalAmountPaidBase = baseAmount + flatFee + pctFeeBase

            // Hidden spread profit = Base Amount Customer Pays - Base Amount Needed at Market Rate
            val hiddenSpreadProfitBase = baseAmount - baseAmountMkt

            val grossFeeProfitBase = flatFee + pctFeeBase

            // Agent Costs calculated based on selected Deduction Base and rounded to nearest whole number
            val pctAgentCostBase = if (deductionBase == DeductionBase.ON_DELIVERED_TARGET) {
                val trueCostInEur = if (direction == TransferDirection.EUR_TO_USD) {
                    targetAmount / marketRate
                } else {
                    baseAmount
                }
                kotlin.math.round(trueCostInEur * (pctAgentCostPercent / 100.0))
            } else {
                if (direction == TransferDirection.EUR_TO_USD) {
                    kotlin.math.round(baseAmount * (pctAgentCostPercent / 100.0))
                } else {
                    val eurAmount = targetAmountCust
                    val pctAgentCostEur = eurAmount * pctAgentCostPercent / 100.0
                    kotlin.math.round(pctAgentCostEur / customerRate)
                }
            }

            val deliveryFeeBase = if (direction == TransferDirection.EUR_TO_USD) deliveryFee / marketRate else deliveryFee
            val totalAgentCostBase = flatAgentCost + pctAgentCostBase
            val totalProfitBase = grossFeeProfitBase + hiddenSpreadProfitBase
            val netProfitBase = totalProfitBase - totalAgentCostBase - deliveryFeeBase

            return CalculationResults(
                totalAmountPaidBase = totalAmountPaidBase,
                totalAmountReceivedTarget = targetAmount,
                baseAmount = baseAmount,
                flatFeeBase = flatFee,
                pctFeeBase = pctFeeBase,
                deliveryFeeTarget = deliveryFee,
                grossFeeProfitBase = grossFeeProfitBase,
                hiddenSpreadProfitBase = hiddenSpreadProfitBase,
                flatAgentCostBase = flatAgentCost,
                pctAgentCostBase = pctAgentCostBase,
                totalAgentCostBase = totalAgentCostBase,
                netProfitBase = netProfitBase
            )
        }

        fun calculateCustomDeal(
            baseAmountReceived: Double,
            targetAmountDelivered: Double,
            marketRate: Double,
            flatAgentCost: Double,
            pctAgentCostPercent: Double,
            direction: TransferDirection,
            deductionBase: DeductionBase = DeductionBase.ON_RECEIVED_BASE
        ): CalculationResults {
            // Actual market cost in base currency of target amount delivered
            val baseMarketCost = targetAmountDelivered / marketRate

            // Percentage agent cost calculated based on Deduction Base and rounded to nearest whole number
            val pctAgentCostBase = if (deductionBase == DeductionBase.ON_DELIVERED_TARGET) {
                val trueCostInEur = if (direction == TransferDirection.EUR_TO_USD) {
                    targetAmountDelivered / marketRate
                } else {
                    baseAmountReceived
                }
                kotlin.math.round(trueCostInEur * (pctAgentCostPercent / 100.0))
            } else {
                if (direction == TransferDirection.EUR_TO_USD) {
                    kotlin.math.round(baseAmountReceived * (pctAgentCostPercent / 100.0))
                } else {
                    val eurAmount = targetAmountDelivered
                    val pctAgentCostEur = eurAmount * pctAgentCostPercent / 100.0
                    kotlin.math.round(pctAgentCostEur / marketRate)
                }
            }

            val totalAgentCostBase = flatAgentCost + pctAgentCostBase
            
            // For custom deal, the spread profit is the total difference between amount paid and actual market cost before partner costs
            val hiddenSpreadProfitBase = baseAmountReceived - baseMarketCost

            val totalAmountPaidBase = baseAmountReceived
            val totalAmountReceivedTarget = targetAmountDelivered
            val netProfitBase = hiddenSpreadProfitBase - totalAgentCostBase

            return CalculationResults(
                totalAmountPaidBase = totalAmountPaidBase,
                totalAmountReceivedTarget = totalAmountReceivedTarget,
                baseAmount = baseAmountReceived,
                flatFeeBase = 0.0,
                pctFeeBase = 0.0,
                deliveryFeeTarget = 0.0,
                grossFeeProfitBase = 0.0,
                hiddenSpreadProfitBase = hiddenSpreadProfitBase,
                flatAgentCostBase = flatAgentCost,
                pctAgentCostBase = pctAgentCostBase,
                totalAgentCostBase = totalAgentCostBase,
                netProfitBase = netProfitBase
            )
        }
    }

    /**
     * Executes the financial calculations reactively based on UI states.
     */
    fun calculateProfit() {
        val state = _uiState.value
        val amountVal = state.amountInput.toDoubleOrNull()
        val marketRateVal = state.marketRate.toDoubleOrNull()
        val customerRateVal = state.customerRate.toDoubleOrNull()
        
        val flatFeeVal = state.flatFee.toDoubleOrNull() ?: 0.0
        val pctFeeVal = state.pctFee.toDoubleOrNull() ?: 0.0
        val deliveryFeeVal = state.deliveryFee.toDoubleOrNull() ?: 0.0
        
        val flatAgentCostVal = state.flatAgentCost.toDoubleOrNull() ?: 0.0
        val pctAgentCostVal = state.pctAgentCost.toDoubleOrNull() ?: 0.0

        if (amountVal == null || amountVal <= 0.0) {
            _uiState.update { 
                it.copy(
                    calculationError = CalculationError.INVALID_BASE, 
                    calculationResults = null
                ) 
            }
            return
        }

        if (marketRateVal == null || marketRateVal <= 0.0 || (state.transferMode != TransferMode.CUSTOM_DEAL && (customerRateVal == null || customerRateVal <= 0.0))) {
            _uiState.update { 
                it.copy(
                    calculationError = CalculationError.INVALID_RATE, 
                    calculationResults = null
                ) 
            }
            return
        }

        val results = when (state.transferMode) {
            TransferMode.SEND_BASE -> {
                calculateSendBase(
                    baseAmount = amountVal,
                    marketRate = marketRateVal,
                    customerRate = customerRateVal ?: 0.0,
                    flatFee = flatFeeVal,
                    pctFeePercent = pctFeeVal,
                    deliveryFee = deliveryFeeVal,
                    flatAgentCost = flatAgentCostVal,
                    pctAgentCostPercent = pctAgentCostVal,
                    direction = state.transferDirection,
                    isFeeInclusive = state.isFeeInclusive,
                    deductionBase = state.deductionBase
                )
            }
            TransferMode.RECEIVE_TARGET -> {
                calculateReceiveTarget(
                    targetAmount = amountVal,
                    marketRate = marketRateVal,
                    customerRate = customerRateVal ?: 0.0,
                    flatFee = flatFeeVal,
                    pctFeePercent = pctFeeVal,
                    deliveryFee = deliveryFeeVal,
                    flatAgentCost = flatAgentCostVal,
                    pctAgentCostPercent = pctAgentCostVal,
                    direction = state.transferDirection,
                    deductionBase = state.deductionBase
                )
            }
            TransferMode.CUSTOM_DEAL -> {
                val targetAmountVal = state.customDealTargetAmount.toDoubleOrNull()
                if (targetAmountVal == null || targetAmountVal <= 0.0) {
                    _uiState.update { 
                        it.copy(
                            calculationError = CalculationError.INVALID_BASE, 
                            calculationResults = null
                        ) 
                    }
                    return
                }
                calculateCustomDeal(
                    baseAmountReceived = amountVal,
                    targetAmountDelivered = targetAmountVal,
                    marketRate = marketRateVal,
                    flatAgentCost = flatAgentCostVal,
                    pctAgentCostPercent = pctAgentCostVal,
                    direction = state.transferDirection,
                    deductionBase = state.deductionBase
                )
            }
        }

        if (results.totalAmountReceivedTarget < 0.0) {
            _uiState.update { 
                it.copy(
                    calculationError = CalculationError.FEE_EXCEED, 
                    calculationResults = null
                ) 
            }
            return
        }

        _uiState.update {
            it.copy(
                calculationError = CalculationError.NONE,
                calculationResults = results
            )
        }
    }
}

/**
 * Global helper to translate currency code.
 */
fun getCurrencyName(code: String, language: Language): String {
    return if (language == Language.AR) {
        when (code) {
            "EUR" -> "اليورو"
            "USD" -> "الدولار الأمريكي"
            "TRY" -> "الليرة التركية"
            "IQD" -> "الدينار العراقي"
            "GBP" -> "الجنيه الإسترليني"
            "CAD" -> "الدولار الكندي"
            "AUD" -> "الدولار الأسترالي"
            "JPY" -> "الين الياباني"
            "SAR" -> "الريال السعودي"
            "AED" -> "الدرهم الإماراتي"
            "KWD" -> "الدينار الكويتي"
            else -> code
        }
    } else {
        when (code) {
            "EUR" -> "Euro"
            "USD" -> "US Dollar"
            "TRY" -> "Turkish Lira"
            "IQD" -> "Iraqi Dinar"
            "GBP" -> "British Pound"
            "CAD" -> "Canadian Dollar"
            "AUD" -> "Australian Dollar"
            "JPY" -> "Japanese Yen"
            "SAR" -> "Saudi Riyal"
            "AED" -> "UAE Dirham"
            "KWD" -> "Kuwaiti Dinar"
            else -> code
        }
    }
}

/**
 * Global helper to get Flag URL from CDN.
 */
fun getFlagUrl(currencyCode: String): String {
    val code = when (currencyCode.uppercase()) {
        "EUR" -> "eu"
        "USD" -> "us"
        "TRY" -> "tr"
        "IQD" -> "iq"
        "GBP" -> "gb"
        "CAD" -> "ca"
        "AUD" -> "au"
        "JPY" -> "jp"
        "SAR" -> "sa"
        "AED" -> "ae"
        "KWD" -> "kw"
        else -> currencyCode.take(2).lowercase()
    }
    return "https://flagcdn.com/w80/$code.png"
}
