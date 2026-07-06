# TopalFX Pro - Remittance & Exchange Margin Calculator

**TopalFX Pro** is an advanced, premium-designed financial calculator app developed in Kotlin and Jetpack Compose. Built specifically for remittance offices and exchange agencies, it integrates live exchange rates from the Frankfurter API with complex, custom fee configurations and a hidden profit ledger to optimize business operations.

---

## 🌟 Core Features

### 1. Live Exchange Rates Board
* Automatically polls live market exchange rates for key currency pairs (e.g. `EUR/USD`, `USD/TRY`, etc.).
* Displays price trends (upward/downward) and percentage changes dynamically.
* Tap any rate card on the live board to automatically load it as the calculator's current rate.

### 2. Multi-Mode Calculations
* **Mode A: Send Exact (إرسال مبلغ محدد)**: The customer pays a specific base amount. Calculate the exact target amount the receiver gets and the total cost.
  * **Fees Toggle Option**: Support choosing whether transfer fees are charged **on top** (exclusive) or **deducted from** the input amount (inclusive).
* **Mode B: Receive Exact (استلام مبلغ محدد)**: The customer wants the receiver to get an exact target amount. Calculate the exact base cost the customer pays.
* **Mode C: Custom Deal (صفقة خاصة)**: A flat, negotiated package where the customer pays a fixed amount and the receiver gets a fixed amount with *no visible fees* displayed on the receipt.

### 3. Manual Rate Override (Offline Fallback)
* If the API fails to load live rates or in case of offline operation, both the **Market Rate** and **Customer Rate** fields are fully editable.
* Supports clicking "Sync Rate" to copy the manually entered Market Rate into the Customer Rate field.
* Includes a "Clear Fields" (`تفريغ الحقول`) button to quickly reset amount fields for the next transaction.

### 4. Collapsible Secure Admin Dashboard
A private panel for operators displaying the actual financial outcomes of each transaction:
* **Gross Fee Profit**: Profit from customer-facing fees.
* **Hidden Spread Profit**: Profit realized from the markup margin between Market Rate and Customer Rate.
* **Agent Costs**: Partner delivery costs (flat fee + percentage cost calculated strictly on the EUR principal).
* **Dual-Currency Net Profit**: Displays the final net profit in both the base currency (e.g. EUR) and target currency (e.g. USD) simultaneously.

---

## 📊 Business Logic & Formulas

### Mode A: Send Exact (Fees Deducted / Inclusive)
When **Fees Deducted** is selected:
$$Principal = \frac{Base_{total} - FlatFee}{1 + \frac{PctFeePercent}{100}}$$
* Output target delivered: $Target_{delivered} = Principal \times CustomerRate - DeliveryFee$

### Mode C: Custom Deal (Flat Exchange)
* Actual market cost of target delivered: $MarketCost_{base} = \frac{Target_{delivered}}{MarketRate}$
* Hidden Spread Profit: $SpreadProfit_{base} = Base_{received} - MarketCost_{base}$
* Net Profit: $NetProfit_{base} = SpreadProfit_{base} - AgentCost_{flat} - AgentCost_{pct\_base}$

---

## 🛠️ Technology Stack & Architecture
* **Language**: Kotlin
* **UI Toolkit**: Jetpack Compose (Modern responsive layouts, dark mode, glassmorphism, Cairo Typography)
* **Architecture**: MVVM (Model-View-ViewModel) with unidirectional reactive data streams using StateFlow
* **Network client**: Retrofit 2 + Gson Converter
* **Build tool**: Gradle Kotlin DSL (`build.gradle.kts`) with release packaging signing configurations

---

## 🚀 Getting Started

### 1. Credentials Setup
To build the signed release APK, copy `gradle.properties.template` to a new file named `gradle.properties` in the project root:
```properties
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_PASSWORD=your_key_password
RELEASE_KEY_ALIAS=topalfx_key
```

### 2. Building the Project
* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
* **Build Signed Release APK**:
  ```bash
  ./gradlew assembleRelease
  ```
  The signed, optimized release package will be generated at `app/build/outputs/apk/release/app-release.apk`.
