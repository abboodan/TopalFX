# TopalFX Pro - Version History

This ledger tracks the version increments and changelogs of the TopalFX Pro remittance engine.

---

## [v1.5.0] - 2026-07-22
### Added
- **Deduction Base Selection (أساس التنزيل)**: Toggle added in Admin Panel supporting Option A ("On Received Amount (EUR)") and Option B ("On Delivered Target (USD)").
- **Office Cost Rounding**: Office percentage cost (`pctAgentCostBase`) is strictly rounded to whole numbers using `Math.round()`.
- **Advanced Expense Ledger**: External delivery fees in base currency (`deliveryFee / marketRate`) are properly deducted from net profit.

---

## [v1.6.0] - 2026-07-22
### Added
- **Same-Currency Remittances (EUR ➔ EUR & USD ➔ USD)**: Added support for transfers within the same currency (e.g., EUR to EUR or USD to USD).
- **Static Exchange Rate Lock**: Automatically locks market and customer rates to `1.0000` for same-currency transfers.
- **4-Way Transfer Direction Selector**: Expanded transfer direction controls to toggle seamlessly between EUR->USD, USD->EUR, EUR->EUR, and USD->USD.

---

## [v1.5.0] - 2026-07-22
### Added
- Enlarge the centered application branding logo to 100.dp with rounded corners and a more prominent border.

---

## [v1.4.0] - 2026-07-08
### Added
- **Binance-inspired Currency Watchlist**: Added a premium scrollable exchange rate watchlist styled with a deep dark aesthetic.
- **Double Overlapping Flag Badges**: Added base and quote country flag overlays using Coil AsyncImage.
- **Dynamic Sparkline Price Charts**: Custom 24h trend lines drawn on Canvas, dynamically colored Green (#00C853) for positive or Red (#FF1744) for negative shifts.

### Changed
- Watchlist container background made transparent to integrate seamlessly with the app's dark-blue background.

---

## [v1.3.0] - 2026-07-06
### Added
- **TopalFX Pro Branding Header**: Centered branding header displaying the circular app logo, static name "TopalFX Pro", and version number.
- **Fee Inclusive / Exclusive Toggle**: Added option in Mode A (Send Exact) to either charge fees on top of the input amount (exclusive) or deduct fees from the input amount (inclusive).
- **Manual Market Rate Override**: Support manual editing of the Market Rate input field to allow fully offline calculations when the live API fetch fails.
- **Clear Fields Shortcut**: A button to instantly clear transaction amount fields and calculation results.
- **Signed Release Packaging**: Integrated signing configuration inside build script to package installable release APKs directly.

### Changed
- Improved RTL arrow layout symbols to point to the left (`←`) in Arabic translation, matching reading direction.

---

## [v1.2.0] - 2026-07-02
### Added
- **Mode C: Custom Deal (Flat Exchange)**: Special calculation mode with hidden calculations (no fees shown to customer).
- Hidden profits calculations inside Admin Panel based on market costs and internal agent percentage/flat costs.

---

## [v1.1.0] - 2026-06-29
### Added
- Bi-directional transfers (EUR to USD / USD to EUR).
- RTL layout direction adjustments to prevent negative currency symbol scrambling.
- Stacked partner configurations in Admin Panel to support vertical scrolling.

---

## [v1.0.0] - 2026-06-25
- Initial release of Remittance Calculator with live rates from Frankfurter API.
