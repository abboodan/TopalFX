package com.topal.fx

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topal.fx.R
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TopalFXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemittanceCalculatorScreen(viewModel)
                }
            }
        }
    }
}

// Custom Google Cairo Font Family Definition
val CairoFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
    Font(R.font.cairo_light, FontWeight.Light)
)

// Custom Premium Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),      // Emerald Green
    secondary = Color(0xFF6366F1),    // Indigo
    tertiary = Color(0xFF06B6D4),     // Cyan
    background = Color(0xFF0F172A),   // Deep Slate Gray
    surface = Color(0xFF1E293B),      // Card Surface
    error = Color(0xFFF43F5E),        // Rose Red
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFE2E8F0)
)

@Composable
fun TopalFXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            headlineMedium = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.5.sp
            ),
            titleLarge = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp
            ),
            titleMedium = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            bodySmall = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            labelLarge = TextStyle(
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        ),
        content = content
    )
}

// Localization Model
class AppStrings(
    val title: String,
    val subtitle: String,
    val liveBoardLabel: String,
    val rateCopiedToast: String,
    val directionEurToUsd: String,
    val directionUsdToEur: String,
    val sendExactMode: String,
    val receiveExactMode: String,
    val customDealMode: String,
    
    // Form Inputs
    val marketRateLabel: String,
    val customerRateLabel: String,
    val amountInputSendLabel: String,
    val amountInputReceiveLabel: String,
    val amountInputCustomBaseLabel: String,
    val amountInputCustomTargetLabel: String,
    val flatFeeLabel: String,
    val pctFeeLabel: String,
    val deliveryFeeLabel: String,
    
    // UI Helpers
    val calculateButton: String,
    val resetRateLabel: String,
    val rateFetchErrorText: String,
    
    // Calculator Results
    val calcResults: String,
    val totalAmountPaidLabel: String,
    val totalAmountReceivedLabel: String,
    val feesBreakdownTitle: String,
    val remittanceFee: String,
    val deliveryFeeEurLabel: String,
    val officeCostEurLabel: String,
    val totalExpenses: String,
    
    // Admin Dashboard
    val adminPanelTitle: String,
    val grossFeeProfitLabel: String,
    val hiddenSpreadProfitLabel: String,
    val totalProfitLabel: String,
    val agentCostsTitle: String,
    val flatAgentCostLabel: String,
    val pctAgentCostLabel: String,
    val netProfitLabel: String,
    
    // Settings Dialog
    val settingsTitle: String,
    val saveButton: String,
    val cancelButton: String,
    val settingsSaved: String,
    val liveRatesTitle: String,
    val addCustomPairTitle: String,
    val fromLabel: String,
    val toLabel: String,
    val addPairButton: String,
    
    // Validation Errors
    val feeExceedError: String,
    val invalidBaseError: String,
    val invalidRateError: String,
    val rateWarningSpread: String,

    // Deduction Base
    val deductionBaseLabel: String,
    val optionAOnReceived: String,
    val optionBOnDelivered: String,

    // Same Currency Directions
    val directionEurToEur: String,
    val directionUsdToUsd: String,

    // Auto-Updater
    val updatesSectionTitle: String,
    val checkForUpdatesButton: String,
    val updateServerUrlLabel: String,
    val updateAvailableTitle: String,
    val updateNowButton: String,
    val remindMeLaterButton: String,
    val downloadingUpdateLabel: String
)

val EnglishStrings = AppStrings(
    title = "TopalFX Pro",
    subtitle = "Secure Remittance & Margin Exchange Engine",
    liveBoardLabel = "Live FX Ticker Board",
    rateCopiedToast = "Rate loaded into workspace: ",
    directionEurToUsd = "EUR ➔ USD",
    directionUsdToEur = "USD ➔ EUR",
    sendExactMode = "Send Exact",
    receiveExactMode = "Receive Exact",
    customDealMode = "Custom Deal",
    marketRateLabel = "Market Rate (Read-Only)",
    customerRateLabel = "Customer Rate (Editable Spread)",
    amountInputSendLabel = "Customer Sends Amount",
    amountInputReceiveLabel = "Receiver Gets Exact Amount",
    amountInputCustomBaseLabel = "Received from Customer",
    amountInputCustomTargetLabel = "Sent to Receiver",
    flatFeeLabel = "Standard Flat Fee",
    pctFeeLabel = "Percentage Fee (Calculated on EUR)",
    deliveryFeeLabel = "Destination Payout Fee",
    calculateButton = "Process Calculations",
    resetRateLabel = "Sync Rate",
    rateFetchErrorText = "Failed to sync market rate. Verify your connection.",
    calcResults = "Customer Payout Receipt",
    totalAmountPaidLabel = "Total Customer Pays",
    totalAmountReceivedLabel = "Amount Delivered to Receiver",
    feesBreakdownTitle = "Direct Transaction Fees",
    remittanceFee = "Remittance Fee",
    deliveryFeeEurLabel = "External Delivery Fee",
    officeCostEurLabel = "Re-downloading Fee",
    totalExpenses = "Total Expenses (Base)",
    adminPanelTitle = "Margin & Profit Ledger (Admin Only)",
    grossFeeProfitLabel = "Direct Fees Revenue",
    hiddenSpreadProfitLabel = "Hidden Spread Profit",
    totalProfitLabel = "Total Revenue",
    agentCostsTitle = "Outward Agent cost Settings",
    flatAgentCostLabel = "Flat Partner Cost",
    pctAgentCostLabel = "Cost of Re-downloading (on EUR)",
    netProfitLabel = "Ledger Net Profit",
    settingsTitle = "Configure System Defaults",
    saveButton = "Save Defaults",
    cancelButton = "Close",
    settingsSaved = "System default configuration saved.",
    liveRatesTitle = "Live Ticker Pairs Ledger",
    addCustomPairTitle = "Track New Currency Pair",
    fromLabel = "From",
    toLabel = "To",
    addPairButton = "Add Pair",
    feeExceedError = "Total calculated fees exceed the principal amount.",
    invalidBaseError = "Please specify a valid financial principal (> 0).",
    invalidRateError = "Exchange rate must be positive and greater than zero.",
    rateWarningSpread = "Warning: Customer rate exceeds market rate. Spread profit is negative.",
    deductionBaseLabel = "Office Cost Deduction Base",
    optionAOnReceived = "On Received Amount (EUR)",
    optionBOnDelivered = "On Delivered Target (USD)",
    directionEurToEur = "EUR ➔ EUR",
    directionUsdToUsd = "USD ➔ USD",
    updatesSectionTitle = "App Updates & Server Sync",
    checkForUpdatesButton = "Check Updates",
    updateServerUrlLabel = "Update Server JSON Endpoint",
    updateAvailableTitle = "New Update Available!",
    updateNowButton = "Update Now",
    remindMeLaterButton = "Remind Me Later",
    downloadingUpdateLabel = "Downloading update package..."
)

val ArabicStrings = AppStrings(
    title = "TopalFX برو",
    subtitle = "محرك الحوالات المالي المطور وحساب هوامش الأرباح",
    liveBoardLabel = "لوحة أسعار الصرف المباشرة",
    rateCopiedToast = "تم تحميل السعر إلى الحاسبة: ",
    directionEurToUsd = "يورو ← دولار",
    directionUsdToEur = "دولار ← يورو",
    sendExactMode = "إرسال مبلغ محدد",
    receiveExactMode = "استلام مبلغ محدد",
    customDealMode = "صفقة خاصة",
    marketRateLabel = "سعر السوق المباشر (للقراءة فقط)",
    customerRateLabel = "سعر العميل (قابل للتعديل والهامش)",
    amountInputSendLabel = "المبلغ المرسل من العميل",
    amountInputReceiveLabel = "المبلغ المراد إيصاله للمستلم",
    amountInputCustomBaseLabel = "المبلغ المستلم من العميل",
    amountInputCustomTargetLabel = "المبلغ المرسل للمستلم",
    flatFeeLabel = "الرسوم الثابتة للحوالة",
    pctFeeLabel = "الرسوم بالنسبة المئوية (تُحسب على اليورو)",
    deliveryFeeLabel = "أجور تسليم المستلم في الوجهة",
    calculateButton = "إجراء العمليات الحسابية",
    resetRateLabel = "مزامنة السعر",
    rateFetchErrorText = "فشل في تحديث سعر صرف السوق. تحقق من اتصالك.",
    calcResults = "إيصال حساب الحوالة المالي",
    totalAmountPaidLabel = "إجمالي ما يدفعه العميل",
    totalAmountReceivedLabel = "المبلغ النهائي الواصل للمستلم",
    feesBreakdownTitle = "تفاصيل الرسوم المباشرة",
    remittanceFee = "أجر الحوالة",
    deliveryFeeEurLabel = "أجور وكيل التسليم",
    officeCostEurLabel = "أجور إعادة تنزيل المبلغ",
    totalExpenses = "إجمالي المصاريف (الأساسية)",
    adminPanelTitle = "لوحة الأرباح والهوامش المخفية (خاص بالإدارة)",
    grossFeeProfitLabel = "أرباح الرسوم المباشرة",
    hiddenSpreadProfitLabel = "أرباح الهامش المخفية (الفرق)",
    totalProfitLabel = "إجمالي أرباح العملية",
    agentCostsTitle = "إعدادات تكلفة الشركاء والوكلاء",
    flatAgentCostLabel = "تكلفة الشريك الثابتة",
    pctAgentCostLabel = "أجور إعادة التنزيل للوكيل (على اليورو)",
    netProfitLabel = "صافي ربح العملية النهائي",
    settingsTitle = "الإعدادات الافتراضية للنظام",
    saveButton = "حفظ الإعدادات",
    cancelButton = "إغلاق",
    settingsSaved = "تم حفظ التكوين الافتراضي بنجاح.",
    liveRatesTitle = "إدارة أزواج العملات في اللوحة",
    addCustomPairTitle = "تتبع زوج عملات جديد",
    fromLabel = "من",
    toLabel = "إلى",
    addPairButton = "إضافة",
    feeExceedError = "تتجاوز الرسوم المقتطعة قيمة المبلغ الأساسي المرسل.",
    invalidBaseError = "يرجى تحديد مبلغ مالي صحيح أكبر من الصفر.",
    invalidRateError = "يجب أن يكون سعر الصرف المعتمد قيمة موجبة أكبر من الصفر.",
    rateWarningSpread = "تنبيه: سعر العميل أعلى من سعر السوق المباشر. هامش الربح الخفي خاسر.",
    deductionBaseLabel = "أساس أسلوب إعادة التنزيل",
    optionAOnReceived = "على المبلغ المستلم (يورو)",
    optionBOnDelivered = "على المبلغ الواصل (دولار)",
    directionEurToEur = "يورو ← يورو",
    directionUsdToUsd = "دولار ← دولار",
    updatesSectionTitle = "تحديثات التطبيق وسيرفر المزامنة",
    checkForUpdatesButton = "فحص التحديثات الآن",
    updateServerUrlLabel = "رابط ملف سيرفر التحديثات (JSON)",
    updateAvailableTitle = "تحديث جديد متاح!",
    updateNowButton = "تحديث الآن",
    remindMeLaterButton = "تذكيري لاحقاً",
    downloadingUpdateLabel = "جاري تنزيل ملف التحديث..."
)

/**
 * Helper to format rates based on magnitude.
 */
fun formatRate(rate: Double): String {
    return when {
        rate == 0.0 -> "0.00"
        rate < 0.1 -> String.format(Locale.US, "%.6f", rate)
        rate < 1.0 -> String.format(Locale.US, "%.5f", rate)
        rate < 100.0 -> String.format(Locale.US, "%.4f", rate)
        else -> String.format(Locale.US, "%.2f", rate)
    }
}

@Composable
fun Sparkline(
    points: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 0.0
        val range = max - min
        
        val width = size.width
        val height = size.height
        val padding = 4.dp.toPx()
        val usableHeight = height - 2 * padding
        
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * (width / (points.size - 1))
            val y = if (range == 0.0) {
                height / 2f
            } else {
                height - padding - (((value - min) / range) * usableHeight).toFloat()
            }
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun CurrencyRow(
    rate: CurrencyRate,
    language: Language,
    onClick: () -> Unit
) {
    val isPositive = rate.changePercentage >= 0.0
    val trendColor = if (isPositive) Color(0xFF00C853) else Color(0xFFFF1744)
    val percentageText = String.format(
        Locale.US,
        "%s%.2f%% %s",
        if (isPositive) "+" else "",
        rate.changePercentage,
        if (isPositive) "↑" else "↓"
    )

    // Splitting symbolPair
    val parts = rate.symbolPair.split("/")
    val baseCode = parts.getOrNull(0) ?: ""
    val quoteCode = parts.getOrNull(1) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Section: Circular double flag icons
        Row(
            modifier = Modifier.weight(1.3f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(width = 44.dp, height = 30.dp)
            ) {
                AsyncImage(
                    model = rate.baseCurrencyFlagUrl,
                    contentDescription = baseCode,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .align(Alignment.TopStart)
                )
                AsyncImage(
                    model = getFlagUrl(quoteCode),
                    contentDescription = quoteCode,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = rate.symbolPair,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = rate.fullNamePair,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Center Section: Sparkline Chart
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Sparkline(
                points = rate.trendPoints,
                color = trendColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Right Section: Current rate & Percentage change
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatRate(rate.currentRate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = percentageText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = trendColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceCalculatorScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Select dynamic localized texts
    val strings = if (uiState.language == Language.EN) EnglishStrings else ArabicStrings
    
    // Choose layout direction dynamically
    val layoutDirection = if (uiState.language == Language.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    var showSettingsDialog by remember { mutableStateOf(false) }

    val baseCurrencyCode = when (uiState.transferDirection) {
        TransferDirection.EUR_TO_USD, TransferDirection.EUR_TO_EUR -> "EUR"
        TransferDirection.USD_TO_EUR, TransferDirection.USD_TO_USD -> "USD"
    }
    val targetCurrencyCode = when (uiState.transferDirection) {
        TransferDirection.EUR_TO_USD, TransferDirection.USD_TO_USD -> "USD"
        TransferDirection.USD_TO_EUR, TransferDirection.EUR_TO_EUR -> "EUR"
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Deep Slate
                            Color(0xFF020617)  // Near Black
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Header Controls (Settings Icon only)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settingsTitle,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Centered App Logo, Static Title and Version Number
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TopalFX Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v1.7.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                
                Text(
                    text = strings.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    textAlign = TextAlign.Start
                )

                // Live Exchange Rates Board Title with Refresh Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.liveBoardLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    IconButton(
                        onClick = { viewModel.fetchTickerRates() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (uiState.isFetchingTicker) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Ticker",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Live Exchange Rates Watchlist Board (Deep dark card styled similar to Binance)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            itemsIndexed(uiState.tickerRates) { index, currencyRate ->
                                CurrencyRow(
                                    rate = currencyRate,
                                    language = uiState.language,
                                    onClick = {
                                        viewModel.selectRateFromTicker(currencyRate.currentRate)
                                        Toast.makeText(
                                            context, 
                                            "${strings.rateCopiedToast}${formatRate(currencyRate.currentRate)}", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                if (index < uiState.tickerRates.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFF1E293B),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Calculator Main Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // 1. Transfer Direction Toggle (EUR->USD or USD->EUR)
                        Text(
                            text = strings.subtitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.transferDirection == TransferDirection.EUR_TO_USD) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.onTransferDirectionChanged(TransferDirection.EUR_TO_USD) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.directionEurToUsd,
                                        color = if (uiState.transferDirection == TransferDirection.EUR_TO_USD) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.transferDirection == TransferDirection.USD_TO_EUR) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.onTransferDirectionChanged(TransferDirection.USD_TO_EUR) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.directionUsdToEur,
                                        color = if (uiState.transferDirection == TransferDirection.USD_TO_EUR) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.transferDirection == TransferDirection.EUR_TO_EUR) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.onTransferDirectionChanged(TransferDirection.EUR_TO_EUR) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.directionEurToEur,
                                        color = if (uiState.transferDirection == TransferDirection.EUR_TO_EUR) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.transferDirection == TransferDirection.USD_TO_USD) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.onTransferDirectionChanged(TransferDirection.USD_TO_USD) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.directionUsdToUsd,
                                        color = if (uiState.transferDirection == TransferDirection.USD_TO_USD) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Mode Select Toggle (Send Exact EUR vs Receive Exact USD)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.transferMode == TransferMode.SEND_BASE) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                    .clickable { viewModel.onTransferModeChanged(TransferMode.SEND_BASE) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.sendExactMode,
                                    color = if (uiState.transferMode == TransferMode.SEND_BASE) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.transferMode == TransferMode.RECEIVE_TARGET) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                    .clickable { viewModel.onTransferModeChanged(TransferMode.RECEIVE_TARGET) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.receiveExactMode,
                                    color = if (uiState.transferMode == TransferMode.RECEIVE_TARGET) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.transferMode == TransferMode.CUSTOM_DEAL) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                    .clickable { viewModel.onTransferModeChanged(TransferMode.CUSTOM_DEAL) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.customDealMode,
                                    color = if (uiState.transferMode == TransferMode.CUSTOM_DEAL) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val baseCurrencyIcon = if (baseCurrencyCode == "EUR") Icons.Default.Euro else Icons.Default.AttachMoney
                        val targetCurrencyIcon = if (targetCurrencyCode == "EUR") Icons.Default.Euro else Icons.Default.AttachMoney

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.transferMode == TransferMode.CUSTOM_DEAL) {
                                    if (uiState.language == Language.AR) "تفاصيل الصفقة الخاصة" else "Custom Deal Details"
                                } else {
                                    if (uiState.language == Language.AR) "المبالغ الأساسية للعملية" else "Transaction Amounts"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.clearInputs() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Fields",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.language == Language.AR) "تفريغ الحقول" else "Clear Fields",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Amount Inputs
                        if (uiState.transferMode == TransferMode.CUSTOM_DEAL) {
                            CustomInputField(
                                value = uiState.amountInput,
                                onValueChange = { viewModel.onAmountInputChanged(it) },
                                label = "${strings.amountInputCustomBaseLabel} (${getCurrencyName(baseCurrencyCode, uiState.language)})",
                                icon = baseCurrencyIcon
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CustomInputField(
                                value = uiState.customDealTargetAmount,
                                onValueChange = { viewModel.onCustomDealTargetAmountChanged(it) },
                                label = "${strings.amountInputCustomTargetLabel} (${getCurrencyName(targetCurrencyCode, uiState.language)})",
                                icon = targetCurrencyIcon
                            )
                        } else {
                            val amountLabel = if (uiState.transferMode == TransferMode.SEND_BASE) {
                                "${strings.amountInputSendLabel} (${getCurrencyName(baseCurrencyCode, uiState.language)})"
                            } else {
                                "${strings.amountInputReceiveLabel} (${getCurrencyName(targetCurrencyCode, uiState.language)})"
                            }
                            CustomInputField(
                                value = uiState.amountInput,
                                onValueChange = { viewModel.onAmountInputChanged(it) },
                                label = amountLabel,
                                icon = if (uiState.transferMode == TransferMode.SEND_BASE) baseCurrencyIcon else targetCurrencyIcon
                            )

                            if (uiState.transferMode == TransferMode.SEND_BASE) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (!uiState.isFeeInclusive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { viewModel.onFeeInclusiveChanged(false) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (uiState.language == Language.AR) "الرسوم خارج المبلغ" else "Fees on Top",
                                            color = if (!uiState.isFeeInclusive) Color.White else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (uiState.isFeeInclusive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { viewModel.onFeeInclusiveChanged(true) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (uiState.language == Language.AR) "الرسوم من ضمن المبلغ" else "Fees Deducted",
                                            color = if (uiState.isFeeInclusive) Color.White else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Rate Configurations (Market Rate & Customer Rate Spread)
                        if (uiState.transferMode != TransferMode.CUSTOM_DEAL) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.marketRate,
                                    onValueChange = { viewModel.onMarketRateChanged(it) },
                                    label = { Text(strings.marketRateLabel) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color(0xFF475569)
                                    )
                                )

                                OutlinedTextField(
                                    value = uiState.customerRate,
                                    onValueChange = { viewModel.onCustomerRateChanged(it) },
                                    label = { Text(strings.customerRateLabel) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color(0xFF475569)
                                    )
                                )
                            }

                            val marketRateD = uiState.marketRate.toDoubleOrNull() ?: 0.0
                            if (marketRateD > 0.0) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val adjustmentOptions = listOf(-0.005, -0.010, -0.015)
                                        adjustmentOptions.forEach { offset ->
                                            val targetRate = marketRateD + offset
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        viewModel.onCustomerRateChanged(String.format(Locale.US, "%.4f", targetRate))
                                                    }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "%+.3f", offset),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    viewModel.onCustomerRateChanged(String.format(Locale.US, "%.4f", marketRateD))
                                                }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.resetRateLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            val customerRateD = uiState.customerRate.toDoubleOrNull() ?: 0.0
                            if (marketRateD > 0.0 && customerRateD > marketRateD) {
                                Text(
                                    text = strings.rateWarningSpread,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.marketRate,
                                onValueChange = { viewModel.onMarketRateChanged(it) },
                                label = { Text(strings.marketRateLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Fee Configuration Inputs
                        if (uiState.transferMode != TransferMode.CUSTOM_DEAL) {
                            CustomInputField(
                                value = uiState.flatFee,
                                onValueChange = { viewModel.onFlatFeeChanged(it) },
                                label = "${strings.flatFeeLabel} (${getCurrencyName(baseCurrencyCode, uiState.language)})",
                                icon = baseCurrencyIcon
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CustomInputField(
                                value = uiState.pctFee,
                                onValueChange = { viewModel.onPctFeeChanged(it) },
                                label = strings.pctFeeLabel,
                                icon = Icons.Default.Percent
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CustomInputField(
                                value = uiState.deliveryFee,
                                onValueChange = { viewModel.onDeliveryFeeChanged(it) },
                                label = "${strings.deliveryFeeLabel} (${getCurrencyName(targetCurrencyCode, uiState.language)})",
                                icon = targetCurrencyIcon
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Collapsible secure admin dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAdminPanel() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isAdminPanelExpanded) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.adminPanelTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Icon(
                                imageVector = if (uiState.isAdminPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        AnimatedVisibility(visible = uiState.isAdminPanelExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = strings.agentCostsTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.flatAgentCost,
                                        onValueChange = { viewModel.onFlatAgentCostChanged(it) },
                                        label = { Text(strings.flatAgentCostLabel) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color(0xFF475569)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.pctAgentCost,
                                        onValueChange = { viewModel.onPctAgentCostChanged(it) },
                                        label = { Text(strings.pctAgentCostLabel) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color(0xFF475569)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = strings.deductionBaseLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                            .padding(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (uiState.deductionBase == DeductionBase.ON_RECEIVED_BASE) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { viewModel.onDeductionBaseChanged(DeductionBase.ON_RECEIVED_BASE) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.optionAOnReceived,
                                                color = if (uiState.deductionBase == DeductionBase.ON_RECEIVED_BASE) Color.White else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (uiState.deductionBase == DeductionBase.ON_DELIVERED_TARGET) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { viewModel.onDeductionBaseChanged(DeductionBase.ON_DELIVERED_TARGET) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.optionBOnDelivered,
                                                color = if (uiState.deductionBase == DeductionBase.ON_DELIVERED_TARGET) Color.White else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                uiState.calculationResults?.let { results ->
                                    HorizontalDivider(
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(vertical = 14.dp)
                                    )

                                    // Gross profit from flat + pct fees
                                    ResultRow(
                                        label = strings.grossFeeProfitLabel,
                                        value = formatMoney(results.grossFeeProfitBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )

                                    ResultRow(
                                        label = strings.hiddenSpreadProfitLabel,
                                        value = formatMoney(results.hiddenSpreadProfitBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )

                                    ResultRow(
                                        label = strings.totalProfitLabel,
                                        value = formatMoney(results.grossFeeProfitBase + results.hiddenSpreadProfitBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )

                                    ResultRow(
                                        label = strings.pctAgentCostLabel,
                                        value = formatMoney(results.totalAgentCostBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )

                                    HorizontalDivider(
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )

                                    val marketRateD = uiState.marketRate.toDoubleOrNull() ?: 1.0
                                    val convertedNetProfit = results.netProfitBase * marketRateD
                                    val convertedCurrencyName = getCurrencyName(targetCurrencyCode, uiState.language)
                                    ProfitSection(
                                        label = strings.netProfitLabel,
                                        amount = results.netProfitBase,
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language),
                                        convertedAmount = convertedNetProfit,
                                        convertedCurrency = convertedCurrencyName
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action execution button
                Button(
                    onClick = { viewModel.calculateProfit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calculate",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.calculateButton,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error validation Card
                AnimatedVisibility(
                    visible = uiState.calculationError != CalculationError.NONE,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val errorMsg = when (uiState.calculationError) {
                        CalculationError.INVALID_BASE -> strings.invalidBaseError
                        CalculationError.INVALID_RATE -> strings.invalidRateError
                        CalculationError.FEE_EXCEED -> strings.feeExceedError
                        CalculationError.NONE -> ""
                    }
                    if (errorMsg.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Customer Results Card
                AnimatedVisibility(
                    visible = uiState.calculationResults != null,
                    enter = fadeIn() + slideInVertically()
                ) {
                    uiState.calculationResults?.let { results ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = strings.calcResults,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 14.dp)
                                )

                                // Principal payout results
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = strings.totalAmountPaidLabel,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = formatMoney(results.totalAmountPaidBase),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = getCurrencyName(baseCurrencyCode, uiState.language),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text(
                                            text = strings.totalAmountReceivedLabel,
                                            color = Color(0xFF38BDF8),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = formatMoney(results.totalAmountReceivedTarget),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = getCurrencyName(targetCurrencyCode, uiState.language),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }

                                if (uiState.transferMode != TransferMode.CUSTOM_DEAL) {
                                    Text(
                                        text = strings.feesBreakdownTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    ResultRow(
                                        label = strings.flatFeeLabel,
                                        value = formatMoney(results.flatFeeBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )
                                    ResultRow(
                                        label = strings.pctFeeLabel,
                                        value = formatMoney(results.pctFeeBase),
                                        currency = getCurrencyName(baseCurrencyCode, uiState.language)
                                    )
                                    ResultRow(
                                        label = strings.deliveryFeeLabel,
                                        value = formatMoney(results.deliveryFeeTarget),
                                        currency = getCurrencyName(targetCurrencyCode, uiState.language)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Settings popup dialog config defaults
    if (showSettingsDialog) {
        val defaults = viewModel.getDefaultSettings()
        var tempCustomerFee by remember(showSettingsDialog) { mutableStateOf(defaults.first) }
        var tempDeliveryFee by remember(showSettingsDialog) { mutableStateOf(defaults.second) }
        var tempOfficePercentage by remember(showSettingsDialog) { mutableStateOf(defaults.third) }
        
        var customFrom by remember { mutableStateOf("") }
        var customTo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.language == Language.AR) "لغة التطبيق" else "App Language",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Button(
                            onClick = { viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (uiState.language == Language.EN) "العربية" else "English",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedTextField(
                        value = tempCustomerFee,
                        onValueChange = { tempCustomerFee = it },
                        label = { Text(strings.pctFeeLabel) },
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    OutlinedTextField(
                        value = tempDeliveryFee,
                        onValueChange = { tempDeliveryFee = it },
                        label = { Text(strings.deliveryFeeLabel) },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    OutlinedTextField(
                        value = tempOfficePercentage,
                        onValueChange = { tempOfficePercentage = it },
                        label = { Text(strings.pctAgentCostLabel) },
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    
                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        text = strings.liveRatesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    viewModel.getSavedTickerPairs().forEach { pair ->
                        val dummyRate = CurrencyRate(
                            baseCurrencyFlagUrl = "",
                            symbolPair = "${pair.first}/${pair.second}",
                            fullNamePair = "",
                            currentRate = 0.0,
                            changePercentage = 0.0,
                            trendPoints = emptyList()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = getCurrencyName(pair.first, uiState.language),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(text = "/", color = Color(0xFF475569))
                                Text(
                                    text = getCurrencyName(pair.second, uiState.language),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.deleteTickerPair(dummyRate) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Pair",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = strings.addCustomPairTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customFrom,
                            onValueChange = { customFrom = it },
                            label = { Text(strings.fromLabel) },
                            placeholder = { Text("GBP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        OutlinedTextField(
                            value = customTo,
                            onValueChange = { customTo = it },
                            label = { Text(strings.toLabel) },
                            placeholder = { Text("TRY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        Button(
                            onClick = {
                                if (customFrom.trim().isNotEmpty() && customTo.trim().isNotEmpty()) {
                                    viewModel.addTickerPair(customFrom, customTo)
                                    Toast.makeText(context, "Pair added!", Toast.LENGTH_SHORT).show()
                                    customFrom = ""
                                    customTo = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(strings.addPairButton, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = strings.updatesSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = uiState.updateUrl,
                        onValueChange = { viewModel.onUpdateUrlChanged(it) },
                        label = { Text(strings.updateServerUrlLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.language == Language.AR) "الإصدار الحالي: v1.7.0" else "Current Version: v1.7.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Button(
                            onClick = { viewModel.checkForUpdates(silent = false) },
                            enabled = !uiState.isCheckingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text(strings.checkForUpdatesButton, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.updateErrorMessage != null) {
                        Text(
                            text = uiState.updateErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveDefaultSettings(
                            pctFee = tempCustomerFee,
                            deliveryFee = tempDeliveryFee,
                            pctAgentCost = tempOfficePercentage
                        )
                        Toast.makeText(context, strings.settingsSaved, Toast.LENGTH_SHORT).show()
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.saveButton, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text(strings.cancelButton)
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Auto-Update Available Dialog
    if (uiState.showUpdateDialog && uiState.appUpdateInfo != null) {
        val info = uiState.appUpdateInfo!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strings.updateAvailableTitle} (${info.versionName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (info.changelog.isNotEmpty()) {
                        Text(
                            text = if (uiState.language == Language.AR) "التغييرات في هذا الإصدار:" else "Changelog:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = info.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    if (uiState.isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${strings.downloadingUpdateLabel} ${(uiState.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = uiState.downloadProgress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFF334155)
                        )
                    }

                    if (uiState.updateErrorMessage != null) {
                        Text(
                            text = uiState.updateErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startDownloadAndUpdate(context) },
                    enabled = !uiState.isDownloadingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.updateNowButton, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUpdateDialog() },
                    enabled = !uiState.isDownloadingUpdate
                ) {
                    Text(strings.remindMeLaterButton, color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFF475569)
        )
    )
}

@Composable
fun FeeTypeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

// BiDi Scaffold Result Row
@Composable
fun ResultRow(
    label: String,
    value: String,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = currency,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// BiDi Scaffold Profit Row
@Composable
fun ProfitSection(
    label: String,
    amount: Double,
    currency: String,
    convertedAmount: Double? = null,
    convertedCurrency: String? = null
) {
    val isPositive = amount >= 0
    val color = if (isPositive) Color(0xFF10B981) else Color(0xFFF43F5E)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE2E8F0),
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${if (isPositive) "+" else ""}${formatMoney(amount)}",
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 17.sp
                )
                Text(
                    text = currency,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 17.sp
                )
            }
            if (convertedAmount != null && convertedCurrency != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${formatMoney(convertedAmount)}",
                        fontWeight = FontWeight.Medium,
                        color = color.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = convertedCurrency,
                        fontWeight = FontWeight.Medium,
                        color = color.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatMoney(value: Double): String {
    return String.format(Locale.US, "%,.2f", value)
}

@Preview(showBackground = true)
@Composable
fun PreviewCalculator() {
    TopalFXTheme {
        RemittanceCalculatorScreen(viewModel = MainViewModel(LocalContext.current.applicationContext as android.app.Application))
    }
}
