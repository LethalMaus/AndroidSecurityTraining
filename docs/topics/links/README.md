# 5. App links & deep links

![Deep Links screen preview](../../../app/src/test/snapshots/images/__DeepLinksHomePreview.png)

#### Where in code
  - Topic manifest: `app/src/links/AndroidManifest.xml`
  - Secure helper: `app/src/secure/java/.../deeplink/SecureDeepLinkHelper.kt`
  - Activity UI: `app/src/links/java/.../DeepLinksActivity.kt`
  - Website DAL file: `./.well-known/assetlinks.json` (already includes base, .secure, .vuln packages)

#### Lab guide (hands-on)
  Goal: See how verified App Links are accepted by the secure build and how broad/unverified links are rejected (secure) but accepted (vuln).

  A) Build and install variants
  - Secure:
    - Android Studio: select Build Variant `secureLinksDebug` and Run
    - CLI: `./gradlew :app:installSecureLinksDebug`
    - Package: `dev.jamescullimore.android_security_training.secure`
  - Vulnerable:
    - Android Studio: select Build Variant `vulnLinksDebug` and Run
    - CLI: `./gradlew :app:installVulnLinksDebug`
    - Package: `dev.jamescullimore.android_security_training.vuln`

  B) Test a verified App Link (secure should accept)
  - Command:
    ```
    adb shell am start -a android.intent.action.VIEW -d "https://lethalmaus.github.io/AndroidSecurityTraining/welcome?code=abc&state=123"
    ```
  - Expected (secure): DeepLinks screen shows validated=true; result "Accepted … (code redacted)".
  - Expected (vuln): Also accepts because it’s https; uses looser checks.

  C) Test an unverified/custom host (secure should reject, vuln accepts)
  - Command:
    ```
    adb shell am start -a android.intent.action.VIEW -d "https://lab.example.com/welcome?code=abc&state=123"
    ```
  - Expected (secure): "Rejected: invalid scheme/host/path" and no navigation.
  - Expected (vuln): Treats as acceptable and echoes parameters.

  D) Toggle between URLs inside the app
  - Open the app UI and use the "Switch to Verified/Unverified URL" button.
  - Use "Simulate Incoming VIEW" and "Navigate Internally (Secure Path)" to compare behavior.

  E) If App Links don’t auto‑verify
  - Ensure the app that should handle links is the default: open the verified URL in Chrome and choose the app; if a chooser appears, long‑press to always open.
  - Clear defaults if needed: Settings → Apps → "Open by default" → Clear.
  - Check verification state (Android 12+): `adb shell pm get-app-links dev.jamescullimore.android_security_training.secure`
  - Uninstall all variants to reset link handling:
    ```
    adb uninstall dev.jamescullimore.android_security_training
    adb uninstall dev.jamescullimore.android_security_training.secure
    adb uninstall dev.jamescullimore.android_security_training.vuln
    ```
  - Note: The provided assetlinks.json includes all three package IDs for convenience during demos.

#### Vulnerable custom-scheme parsing demo (why this is dangerous)
- Goal: Observe how a naive prefix check on a non-canonicalized path can be abused.
- Build: vulnLinksDebug (package: dev.jamescullimore.android_security_training.vuln)
- Activity: DeepLinksActivity (launcher for the links topic)

A) Send a benign custom-scheme URL (accepts a token)
```
adb shell am start -a android.intent.action.VIEW -d "ast://dev.jamescullimore/AndroidSecurityTraining/open/?token=abc123"
```
Expected:
- App opens the Deep Links screen.
- "Received Intent" shows values like:
  - path=/AndroidSecurityTraining/open/
  - canonicalPath=/AndroidSecurityTraining/open
  - naiveAccept(prefix '/AndroidSecurityTraining/open')=true
  - params: token=abc123

B) Send a canonicalized malicious URL using .. (path traversal/confused routing)
```
adb shell am start -a android.intent.action.VIEW -d "ast://dev.jamescullimore/AndroidSecurityTraining/open/../private/secret"
```
Expected (vulnerable behavior):
- App still "accepts" because it checks only that the RAW path starts with /AndroidSecurityTraining/open.
- UI shows:
  - path=/AndroidSecurityTraining/open/../private/secret
  - canonicalPath=/AndroidSecurityTraining/private/secret
  - naiveAccept(prefix '/AndroidSecurityTraining/open')=true

Why this is dangerous
- The decision uses a naive prefix check without canonicalizing dot segments (.., .). An attacker can craft a path that appears to start with an allowed prefix but resolves to a different route when canonicalized.
- Impact examples:
  - Route confusion: reach internal/private handlers (e.g., /private/secret) gated behind an intended /open prefix.
  - Policy bypass: trigger actions or expose data mapped to unintended paths.
  - Data trust: the vulnerable helper also echoes untrusted parameters (e.g., token) which can aid phishing or log injection.

How to fix (secure approach)
- Always normalize/canonicalize the path before validation, then validate against a strict allowlist.
- Validate scheme, host, and path prefixes explicitly; reject anything unexpected.
- Prefer verified App Links for https domains and avoid trusting custom schemes for sensitive flows.
- Don’t echo secrets back to logs/UI; treat deep link params as untrusted input.

#### Best practices
  - Prefer verified app links (assetlinks.json) for https domains.
  - Validate schemes, hosts, and path prefixes explicitly; reject unexpected ones.
  - Avoid exporting components unless required; use `android:exported="false"` by default.

#### Extra reading
  - Verify Android App Links: https://developer.android.com/training/app-links/verify-site-associations
  - Deep links documentation: https://developer.android.com/training/app-links/deep-linking
  - Digital Asset Links: https://developer.android.com/training/app-links/associate-website
  - MASVS‑PLATFORM: https://mas.owasp.org/MASVS/
  - https://developers.google.com/digital-asset-links/v1/getting-started
  - https://owasp.org/www-community/attacks/Path_Traversal
