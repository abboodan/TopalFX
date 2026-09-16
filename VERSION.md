# TopalFX Pro - Version History

This ledger tracks the version increments and changelogs of the TopalFX Pro remittance engine.

---

## [v2.0.0] - 2026-09-16
### Added
- **Two-Way Telegram Bot Remote Control**: Control the notification interceptor remotely using Telegram commands (`/status`, `/pause`, `/resume`, `/ignore <app>`, `/unignore <app>`, `/list_ignored`, `/history [n]`, `/help`).
- **Persistent SQLite Notification Log**: Complete local audit trail (`NotificationDbHelper`) storing all captured notifications in native SQLite (`topalfx_notifications.db`) with timestamps, app names, packages, and forward status.
- **Persistent Settings Configuration**: Automatically save and restore paused state, custom ignored application lists, and Telegram update offsets using `TelegramConfigManager`.
- **Offline & Stale Command Recovery**: Guaranteed chronological order execution for queued commands after power or network loss via offset tracking, with automatic detection and notification for stale commands (> 2 hours old).

---

## [v1.9.1] - 2026-09-16
### Added
- **Anti-Spam Throttling Queue**: Implemented sequential message queue with minimum 1.6s interval pacing to strictly adhere to Telegram's 1 msg/sec limit and prevent bot bans.
- **Smart Deduplication Cache**: Auto-discards identical notifications from the same app within a 45-second window.
- **HTTP 429 Rate-Limit Backoff**: Gracefully parses and honors Telegram `retry_after` responses.

### Fixed
- **Recursive Telegram Loop Prevention**: Explicitly exclude all Telegram package notifications to prevent message feedback loops.
- **Ongoing & System Notification Filter**: Automatically filter out persistent/ongoing system notifications (such as battery charging status and USB connections) and core Android OS noise.

---

## [v1.9.0] - 2026-09-15
### Added
- **Real-Time Telegram Notification Forwarder**: Integrated lightweight background `NotificationListenerService` that intercepts incoming remittances/rate notifications and forwards them instantly to Telegram via a dedicated Bot.
- **Battery Optimization & 24/7 Background Exemption**: Added direct controls in Settings to exempt the app from battery restrictions for non-stop operation even when phone is locked.
- **Diagnostic Settings Dashboard**: Clean permission indicators with single-tap activation for "إشعارات الأسعار والصرف اللحظي" and "استثناء قيود البطارية للتحديث اللحظي".

---

## [v1.8.0] - 2026-07-23
### Added
- **Dynamic Version Comparison**: Dynamically query installed app `versionCode` and `versionName` to prevent infinite update loop popups.
- **GitHub Releases Native Integration**: Automatically parse GitHub Release tags (`v1.8.0`) and assets directly.

---

## [v1.7.0] - 2026-07-23
### Added
- **Self-Hosted In-App Auto-Updater**: Direct in-app update engine fetching version updates from GitHub Releases API.
- **Offline & Network Fallback**: Graceful handling of network timeouts or offline states without app disruption or crashes.
- **Dedicated Settings Update Section**: Added update configuration, current version display, and manual update trigger inside Settings Dialog.
- **Android FileProvider Installer**: Seamless package download and native installer launch with FileProvider security.

---

## [v1.6.0] - 2026-07-22
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
