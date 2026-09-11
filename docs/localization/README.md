# App language behavior

The app follows the system primary language. All `zh` variants (including mainland China, Taiwan, Hong Kong, Hans and Hant) use Simplified Chinese; all other languages use English. Secondary system languages and historical saved in-app selections do not override this rule.

`AppLanguage` wraps each UI activity with a single supported locale. On returning to the foreground, the activity recreates if its language no longer matches the system. Default English resources live in `res/values/strings.xml`; Chinese resources live in `res/values-zh/strings.xml`. Both contain 226 matching entries.

Current screens use resources for navigation, onboarding, network scans and results, sensor and camera tools, tips, settings, subscription and purchase messages. Generated location, device category and plan labels are translated at display time; actual device names, network names, addresses, product identifiers and analytics values retain their original values. Remote legal pages and Google Play UI are provided externally.

Validation:
- Resource key parity and positional format placeholders checked across both languages.
- `AppLanguageTest`: two passing tests covering six Chinese locale variants and eight non-Chinese/undefined locales.
- Debug application and AndroidTest APK compiled. Instrumented UI assertions now read resources instead of hardcoded English.
- Debug application installed successfully on the connected Android phone (`zh-Hans-CN`). Home, navigation and subscription verified in Chinese; screenshots are in `screenshots/`.
- No global device language was changed. Non-Chinese fallback was checked with unit tests, not by switching this phone's language. Instrumented UI tests were not run because this phone blocks installation of the separate test package.
- Final validation: `:app:testDevVersionDebugUnitTest :app:assembleDevVersionDebug :app:lintDevVersionDebug` passed. Lint reports no errors; remaining warnings are recorded in `app/build/reports/lint-results-devVersionDebug.txt`.
