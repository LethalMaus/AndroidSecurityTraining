# Android Security Training

Teach Android security with hands‑on, side‑by‑side examples. Each topic has two flavors: a secure implementation that follows best practices and a deliberately vulnerable one for learning and demos.

This README is concise but complete: purpose, prerequisites, build variants, how to run each of the 10 topics, best‑practice notes, rooted emulator tips, and further reading.

## Table of contents
- [Purpose and scope](#purpose-and-scope)
- [Prerequisites (tools) and before‑you‑start](#prerequisites-tools-and-beforeyoustart-steps)
- [Quick start](#quick-start)
- [MITM proxy quick setup (mitmproxy + emulator)](#mitm-proxy-quick-setup-mitmproxy--emulator)
- [Build variants (how this project is organized)](#build-variants-how-this-project-is-organized)
- [Topics: how to run the labs](#topics-how-to-run-the-labs)
  - [1. Certificate pinning & HTTPS](#1-certificate-pinning--https)
  - [2. End‑to‑end encryption (E2E)](#2-endtoend-encryption-e2e)
  - [3. Reverse‑engineering resistance](#3-reverseengineering-resistance)
  - [4. Runtime permissions](#4-runtime-permissions)
  - [5. App links & deep links](#5-app-links--deep-links)
  - [6. Secure storage](#6-secure-storage)
  - [7. Root/Jailbreak detection](#7-rootjailbreak-detection)
  - [8. WebView & exported components](#8-webview--exported-components)
  - [9. Multi‑user/AAOS considerations](#9-multiuseraaos-considerations)
  - [10. Risk modeling & dangerous defaults](#10-risk-modeling--dangerous-defaults)
- [Frida](#frida)
- [Getting a rooted emulator](#getting-a-rooted-emulator-for-certain-labs)
- [Troubleshooting](#troubleshooting)

## Purpose and scope
- Goal: Help Android developers learn modern security practices through code you can run, inspect, and modify.
- How: Build one topic at a time and toggle secure vs. vulnerable behavior. Use a proxy or other tools to observe differences.
- Outcome: Understand why attacks work against the vulnerable build and how the secure build stops them.

## Prerequisites (tools) and before‑you‑start steps
- Android Studio (latest stable) with Android SDK and emulator images installed.
- Java 11 toolchain (Gradle wrapper config uses JDK 11 compatibility).
- A device or emulator. For interception/root labs, prefer an emulator you control (see rooted emulator section).
- Optional but recommended for labs:
  - mitmproxy, Burp Suite, or OWASP ZAP
  - Wireshark or tcpdump
  - jadx, jadx‐gui and apktool installed
  - DB Browser for SQLite (to inspect pulled SQLite .db files)
  - (optional) [Frida](#frida) 
  - sqlite3 CLI (alternative) — download from https://www.sqlite.org/download.html
- Before you start:
  1) Clone this repo and open it in Android Studio.
  2) Let Gradle sync and index completely.
  3) Decide which topic you want to run first (see Build variants), then pick a secure or vuln profile.
  4) If you plan to demo MITM, configure your proxy and device/emulator networking first.

## Quick start
1) Open the project in Android Studio (latest stable).
2) In Build Variants, choose a pair like `vulnPinning` (to see the issue) then `securePinning` (to see the fix).
3) Run on an emulator or device and follow the on‑screen actions for the selected topic.
4) For network labs, configure your proxy before launching the vulnerable build.

## MITM proxy quick setup (mitmproxy + emulator)
Use this to demo HTTPS interception in the pinning and E2E labs on the Android emulator.

- Start mitmproxy on your host:
  ```
  mitmproxy --listen-host 0.0.0.0 --listen-port 8080
  ```
- Point the Android emulator at your host proxy (Android emulator sees the host at `10.0.2.2`):
  ```
  adb shell settings put global http_proxy 10.0.2.2:8080
  ```
- Install the mitmproxy CA certificate on the emulator (for labs only):
  1) In the emulator browser, visit `http://mitm.it` and download the Android certificate.
  2) Go to Settings → Security → Encryption & credentials → Install a certificate → CA certificate, and select the downloaded file.
     - Note: Secure production builds should distrust user‑installed CAs via Network Security Config; this install is only for lab interception.
- Remove the proxy when you’re done (to restore normal internet access):
  ```
  adb shell settings put global http_proxy :0
  ```
- Optional: Generate an SPKI pin from the mitmproxy CA cert file you downloaded (adjust filename as needed):
  ```
  openssl x509 -in mitmproxy-ca-cert.cer -pubkey -noout
    | openssl pkey -pubin -outform der
    | openssl dgst -sha256 -binary
    | openssl base64
  ```
- Optional: Get an SPKI pin directly from a live host (example: api.github.com):
  ```
  echo | openssl s_client -connect api.github.com:443 -servername api.github.com 2>/dev/null
    | openssl x509 -pubkey -noout
    | openssl pkey -pubin -outform der
    | openssl dgst -sha256 -binary
    | openssl base64
  ```

Tips
- For the pinning lab, you can temporarily add a mitmproxy pin to the `CertificatePinner` (see comments in `SecureNetworkHelper.kt`) to observe how pinning allows or blocks interception.
- For CT mode (`PIN_MODE = "ct"`), no pins are enforced in code; rely on platform trust and Network Security Config with Certificate Transparency enabled. Interception should typically fail unless the user CA is installed.

## Build variants (how this project is organized)
- Two flavor dimensions in `app/build.gradle.kts`:
  - securityProfile: `secure` (best practices) or `vuln` (intentionally unsafe). Release builds are disabled for `vuln`.
  - topic: `pinning`, `e2e`, `re`, `perm`, `links`, `storage`, `root`, `web`, `users`, `risks`.
- The final build variant is `<securityProfile><Topic>`, for example:
  - `securePinning`, `vulnPinning`
  - `secureWeb`, `vulnWeb`, etc.
- Routing is handled by per‑flavor providers so the right helper is compiled for each variant.

## Topics: how to run the labs
Each topic below tells you what to try (lab guide), what “secure” does vs. “vuln”, best practices to take away, and extra reading.

### 1. Certificate pinning & HTTPS
#### Where in code
  - Interface: `app/src/main/java/.../network/NetworkHelper.kt`
  - Secure: `app/src/secure/java/.../network/SecureNetworkHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../network/VulnNetworkHelper.kt`
  - Network Security Config (secure): `app/src/secure/res/xml/network_security_config_client_secure.xml`
#### Lab guide (do this)
  - Quick setup for interception and emulator proxy: see [MITM proxy quick setup (mitmproxy + emulator)](#mitm-proxy-quick-setup-mitmproxy--emulator).
  1) Build `vulnPinning` and route traffic through your proxy. Observe successful MITM using a user‑installed CA and, optionally, cleartext.
  2) Build `securePinning` and repeat. Requests should fail when intercepted or when certificates don’t match pins.
  3) Try rotating the server certificate to demonstrate pin failures.
  4) Optional: Install the mitmproxy CA from `http://mitm.it` on the emulator and generate an SPKI pin for it with the OpenSSL commands in the quick setup section to experiment with pinning/CT behavior.
#### Best practices
  - Prefer HTTPS only; disallow cleartext by default.
  - Use Network Security Config to distrust user CAs for production and limit trust anchors.
  - Apply certificate pinning for high‑risk endpoints; plan operationally for key rotation.
  - Validate hostnames and avoid disabling TLS verification.
#### Extra reading
  - Android Network Security Config: https://developer.android.com/training/articles/security-config
  - OkHttp CertificatePinner: https://square.github.io/okhttp/features/certificates/
  - OWASP MASVS‑NET: https://mas.owasp.org/MASVS/ (networking & crypto controls)
  - Android developers: HTTPS best practices: https://developer.android.com/privacy-and-security/security-ssl

#### Build switches: MANUAL_PIN and PIN_MODE (how the pinning demo behaves)
- Where they live: `app/build.gradle.kts` → secure flavor defines two BuildConfig flags used by the pinning demos:
  - `MANUAL_PIN` (boolean): routes secure builds to a custom TrustManager path meant for training.
    - `true` → uses `ManualPinNetworkHelper` (custom TrustManager + SPKI pin enforcement).
    - `false` → uses `SecureNetworkHelper` (OkHttp + platform trust, optional OkHttp CertificatePinner).
  - `PIN_MODE` (string): selects the demo behavior. Recognized values depend on the path:
    - Common to both paths:
      - `bad` → deliberately wrong pins so requests FAIL (demonstrates pin failure).
      - `good` → correct SPKI pins so requests SUCCEED when not intercepted.
      - `ct` → no code pins; rely on platform trust + Network Security Config with Certificate Transparency (CT). SUCCEED without interception; likely FAIL under MITM.
    - Manual path only (when `MANUAL_PIN = true`):
      - `mitm` → debug‑only helper that trusts the MITM proxy chain and SKIPS SPKI pins so you can intercept HTTPS for the demo. Hostname verification remains on. Intended for emulator/lab use only.

- Defaults in this repo (subject to change):
  - Secure flavor sets `MANUAL_PIN = true` and `PIN_MODE = "mitm"` to make the manual path work with mitmproxy out of the box for the lab.

- How to change modes
  - Edit `app/build.gradle.kts` under the `secure` product flavor and tweak `buildConfigField` values, then rebuild.
  - Example toggles:
    - Use strong, library pinning: set `MANUAL_PIN = false`, `PIN_MODE = "good"` (OkHttp CertificatePinner).
    - CT‑only: set `MANUAL_PIN = false`, `PIN_MODE = "ct"`.
    - Manual pinning demo with failure: set `MANUAL_PIN = true`, `PIN_MODE = "bad"`.
    - Manual pinning demo with success: set `MANUAL_PIN = true`, `PIN_MODE = "good"`.
    - Interception demo (emulator/lab): set `MANUAL_PIN = true`, `PIN_MODE = "mitm"` and enable your proxy (see quick setup below).

- Important notes
  - The manual TrustManager is for training only; rolling your own TM is risky. Prefer OkHttp's `CertificatePinner` or Network Security Config pins for real apps.
  - The `mitm` mode is intentionally insecure and intended for Debug builds only. Release builds should never trust user CAs or bypass pins.
  - In secure production builds, keep user‑installed CAs disabled in Network Security Config and avoid custom TrustManagers.

### 2. End‑to‑end encryption (E2E)
#### Where in code
  - API surface: `app/src/main/java/.../crypto/CryptoHelper.kt`
  - Secure: `app/src/secure/java/.../crypto/SecureCryptoHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../crypto/VulnCryptoHelper.kt`
#### Lab guide
  - Quick setup for interception and emulator proxy: see [MITM proxy quick setup (mitmproxy + emulator)](#mitm-proxy-quick-setup-mitmproxy--emulator).
  1) Run the secure variant and send an encrypted payload to a demo endpoint.
  2) Compare with the vuln variant (e.g., ECB/static key or no integrity).
  3) Modify inputs and show how tampering is detected only with AEAD (GCM/ChaCha20‑Poly1305).
  - Optional: If you want to observe/verify HTTPS transport while doing the E2E lab, install the mitmproxy CA from `http://mitm.it` on the emulator and use the OpenSSL commands from the quick setup section to generate SPKI pins (for either the mitmproxy cert or a live host like `api.github.com`). Remove the proxy with `adb shell settings put global http_proxy :0` when finished.
#### Best practices
  - Use modern AEAD (AES‑GCM or ChaCha20‑Poly1305) with random nonces and include AAD where relevant.
  - Derive keys via a KDF and rotate regularly; never hardcode keys.
  - Use the Android Keystore for long‑term keys; avoid exporting private keys.
#### Extra reading
  - Android Keystore: https://developer.android.com/training/articles/keystore
  - Cryptography best practices (Android): https://developer.android.com/privacy-and-security/crypto
  - OWASP MASVS‑CRYPTO: https://mas.owasp.org/MASVS/
  - NIST SP 800‑38D (GCM): https://csrc.nist.gov/publications/detail/sp/800-38d/final

#### ECB pattern‑leakage demo (vulnerable E2E)
This mini‑lab shows why AES/ECB is insecure: identical 16‑byte plaintext blocks encrypt to identical ciphertext blocks. The vulnerable E2E build intentionally uses ECB under the button labeled “Encrypt Locally (AES‑GCM)” so you can see the leakage without writing code.

- Where in code
  - API surface: `app/src/main/java/.../crypto/CryptoHelper.kt`
  - Vulnerable helper (uses ECB): `app/src/vuln/java/.../crypto/VulnCryptoHelper.kt`
    - `encryptAesGcm(...)` actually calls `Cipher.getInstance("AES/ECB/PKCS5Padding")` and returns zeroed iv/tag for display.
  - E2E screen: `app/src/e2e/java/.../E2EActivity.kt`

- Build this variant
  - Android Studio → Build Variants → Module: app → set Active Build Variant to `vulnE2eDebug`.

- Steps in the app
  1) Launch the app; you should be on the “Encrypting Data Before Transport” (E2E) screen.
  2) In the “JSON Payload” field, paste the following exact, pre‑aligned JSON:

```
{"type":"demo","pad":"x","msg":"ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP"}
```

  Notes:
  - "ABCDEFGHIJKLMNOP" is 16 bytes. It’s repeated many times.
  - The tiny "pad":"x" aligns the start of msg on a 16‑byte boundary in the overall plaintext so blocks line up.
  3) Tap “Encrypt Locally (AES‑GCM)”. In this vulnerable build, that button uses ECB.
  4) Look at the `CT=` line in the on‑screen result. You’ll notice repeating Base64 chunks at regular intervals (every 24 Base64 chars ≈ one 16‑byte block), illustrating ECB’s pattern leakage. The beginning/end blocks differ (JSON keys and PKCS#7 padding), but the middle msg blocks repeat identically.

- Optional verification
  - Copy the CT Base64 from the result. Decode and split into 16‑byte blocks with any hex tool or script; you’ll see many identical blocks in a row.

- Troubleshooting
  - If you don’t see repetition:
    - Confirm the variant is `vulnE2eDebug` (not secure).
    - Use the exact JSON (no extra spaces or newlines).
    - Add more `ABCDEFGHIJKLMNOP` repeats inside `msg` to amplify the effect.

- Contrast with secure build
  - Switch to `secureE2eDebug` and repeat with the same JSON. The secure helper uses real AES‑GCM with a random IV, so ciphertext will not show repeating patterns and includes a valid IV and TAG.

- Why this matters
  - ECB has no IV and no chaining. Identical plaintext blocks under the same key produce identical ciphertext blocks, leaking structure. AEAD modes (e.g., AES‑GCM) provide confidentiality and integrity and use nonces to avoid this leakage.

### 3. Reverse‑engineering resistance
#### Where in code
  - Secure helper: `app/src/secure/java/.../re/SecureReDemoHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../re/VulnReDemoHelper.kt`
  - Gradle flavors for this topic: `re` combined with `secure` or `vuln` (see `app/build.gradle.kts`)

#### Pre‑lab checklist
- Android Studio + adb available
- jadx‐gui and apktool installed
- A keystore for re‑signing (can generate a temporary one)

#### Lab guide (hands‑on)
1) Build the vulnerable RE APK
   - Gradle task:
     ```
     ./gradlew :app:assembleClientVulnReDebug
     ```
   - Output path example:
     ```
     ls app/build/outputs/apk/clientVulnRe/debug/
     ```
   - You should see `vulnRe-debug.apk`.

2) Decompile with JADX (quick source view)
   - Open in JADX:
     ```
     jadx-gui app/build/outputs/apk/.../clientVulnRe-debug.apk
     ```
   - Search for obvious findings:
     - `SUPER_SECRET_API_KEY`
     - `DexClassLoader`
     - `assets/sensitive.txt`
   - Note any hardcoded strings, keys, or debug logs.

3) Decode resources with apktool (smali/manifest/assets)
   - Decode:
     ```
     apktool d clientVulnRe-debug.apk -o out_vuln
     ```
   - Inspect inside `out_vuln/`:
     - `AndroidManifest.xml` (exported components, permissions)
     - `assets/` (plain‑text files or secrets)
     - `smali/` (methods to patch if needed)

4) Extract sensitive assets quickly
   - Without full decode:
     ```
     unzip -p clientVulnRe-debug.apk assets/sensitive.txt
     ```
   - Or pull from a device:
     ```
     adb shell pm path <package>
     adb pull /data/app/.../base.apk
     ```

5) Patch the runtime signature check (tampering demo)
   - Locate the helper (e.g., `SecureReDemoHelper` or similar) in smali and modify the method to return `true`, or patch the decompiled Java and recompile.
   - Rebuild the APK with apktool:
     ```
     apktool b out_vuln -o patched.apk
     ```

6) Re‑sign the modified APK
   - Generate a temporary keystore if you don’t have one:
     ```
     keytool -genkeypair -keystore seminar.jks -alias key0 -storepass changeit -keypass changeit -dname "CN=Seminar,O=Demo,C=US" -keyalg RSA -keysize 2048 -validity 3650
     ```
   - Sign and verify:
     ```
     apksigner sign --ks seminar.jks --ks-pass pass:changeit --key-pass pass:changeit --out patched-signed.apk patched.apk
     apksigner verify --print-certs patched-signed.apk
     ```

7) Install and test the patched APK
   - Replace the original if necessary:
     ```
     adb uninstall <package>
     adb install -r patched-signed.apk
     ```
   - Verify:
     - Does signature/tamper check now pass in the patched build?
     - Check `logcat` for security/tamper logs.

8) Dynamic DEX injection demo (if the vuln UI allows loading from storage)
   - Build a minimal class and convert to DEX:
     ```
     # Example workflow
     javac --release 8 -d build_classes dev/training/dynamic/Hello.java
     jar cvf dynamic.jar -C build_classes dev/training/dynamic/Hello.class
     d8 --output out/ dynamic.jar
     adb shell "mkdir -p /sdcard/Android/data/dev.jamescullimore.android_security_training.vuln/files"
     adb push out/classes.dex /sdcard/Android/data/dev.jamescullimore.android_security_training.vuln/files/dynamic.dex
     ```
   - In the vulnerable app UI, enter just the file name `dynamic.dex` (do not paste a full path). The app will look for this file in its external files directory and then copy it into its internal code cache before loading via DexClassLoader. You can also enter `self` to attempt loading the app's own APK. Discuss risk: unvalidated external code execution.

9) R8 / obfuscation comparison
   - Compare a debug APK vs. a release APK with `minifyEnabled = true`.
   - Observe differences in JADX: identifiers renamed, dead code removed, logs stripped.

#### Group deliverables (for workshops)
- Screenshot of an extracted secret/asset
- Screenshot of the patched app running
- Short write‑up: 3 risks found + 3 mitigations

#### Troubleshooting
- `apktool b` failures → check resources/apktool version mismatch
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → uninstall the app first
- Signature mismatches → verify keystore/alias/passwords
- adb device issues → ensure USB debugging/emulator running

#### Best practices
- Don’t store secrets in the APK; prefer server‑issued, short‑lived tokens.
- Enable R8/ProGuard shrinking/obfuscation for release; strip debug info from release.
- Verify app signature at runtime for critical logic paths; consider Play Integrity as an additional signal.

#### Extra reading
- App signing & verifying: https://developer.android.com/studio/publish/app-signing
- Code shrinking, obfuscation, optimization: https://developer.android.com/studio/build/shrink-code
- OWASP MASVS‑RESILIENCE: https://mas.owasp.org/MASVS/
- Android app reverse engineering overview (docs): https://developer.android.com/privacy-and-security

### 4. Runtime permissions
#### Where in code
- Topic activity: `app/src/perm/java/.../PermActivity.kt`
- Interface: `app/src/main/java/.../perm/PermDemoHelper.kt`
- Vulnerable helper: `app/src/vuln/java/.../perm/VulnPermDemoHelper.kt`
- Secure helper: `app/src/secure/java/.../perm/SecurePermDemoHelper.kt`
- Topic manifest: `app/src/perm/AndroidManifest.xml`

#### What this lab demonstrates
- How Android assigns UIDs/GIDs and ties permissions to app signatures (not package names)
- Risks of exported components without protection (Service/Provider)
- Using a custom signature permission to restrict cross‑app access

#### Build the variants
- Vulnerable (debug):
  ```
  ./gradlew :app:assembleClientVulnPermDebug
  ```
- Secure (debug):
  ```
  ./gradlew :app:assembleClientSecurePermDebug
  ```

#### In‑app steps (PermActivity)
1) Launch the app. You’ll see the Permissions & Packaging screen.
2) Tap “Show UID/GID & Signing Info”
   - Vulnerable: minimal info (no digest)
   - Secure: shows Base64 SHA‑256 signer digest computed at runtime
3) Tap “Start DemoService”
   - Vulnerable: Often succeeds even for external callers if the Service is exported without protection
   - Secure: Designed to be restricted; external calls should fail unless signed with the same key (signature permission)
4) Optionally, enter a Content URI or use the default and tap “Query DemoProvider”
   - Vulnerable: Provider likely exported without protection; query may succeed
   - Secure: Provider should enforce a signature permission and/or not be exported; query should fail for untrusted callers

#### Cross‑app/adb checks (optional, to prove enforcement)
- Find the installed package names:
  ```
  adb shell pm list packages | grep android_security_training
  ```
- Inspect merged manifest and exported state:
  ```
  adb shell dumpsys package dev.jamescullimore.android_security_training.secure | sed -n '1,160p'
  adb shell dumpsys package dev.jamescullimore.android_security_training.vuln | sed -n '1,160p'
  ```
- Try to query the provider from shell (acts as shell UID, not the app):
  ```
  adb shell content query --uri content://dev.jamescullimore.android_security_training.vuln.demo/hello
  ```
  Replace `<package>` with the running variant package (secure or vuln). Expect secure to deny.
- Try to start the service from shell:
  ```
  adb shell am startservice -n <package>/dev.jamescullimore.android_security_training.perm.DemoService
  ```
  Expect secure to deny or require matching signature; vuln may start.

#### Deliverables (for workshops)
- Screenshot of secure vs vuln behavior for Service/Provider attempts
- Short note of what protection stopped access in secure (e.g., signature permission)

#### Troubleshooting
- If `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall first:
  ```
  adb uninstall dev.jamescullimore.android_security_training.secure
  adb uninstall dev.jamescullimore.android_security_training.vuln
  ```
- If provider queries return null in secure: that’s expected when protected; verify logs for `SecurityException`
- If buttons do nothing, ensure you built the `perm` topic variants and that `PermActivity` is the launcher (topic manifest provides it)

#### Best practices
- Request the minimum set, at time‑of‑use; provide clear rationale.
- Avoid exporting components unless necessary; use signature or signatureOrSystem for intra‑suite IPC.
- Prefer explicit intents for internal services and require permissions for any exported components.
- Avoid legacy storage permissions by using scoped storage and intents.

#### Extra reading
- Request app permissions: https://developer.android.com/training/permissions/requesting
- Best practices for permissions: https://developer.android.com/training/permissions/usage
- Data minimization: https://developer.android.com/topic/security/best-practices#data-min
- MASVS‑PLATFORM: https://mas.owasp.org/MASVS/

### 5. App links & deep links
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

### 6. Secure storage
#### Where in code
  - Activity UI: `app/src/storage/java/.../StorageActivity.kt`
  - Secure helper: `app/src/secure/java/.../storage/SecureStorageHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../storage/VulnStorageHelper.kt`
#### Lab guide (hands-on)
  Goal: Compare plaintext vs encrypted storage and observe data on disk.

  A) Build and install variants
  - Secure:
    - Android Studio: Build Variant `secureStorageDebug`
    - CLI: `./gradlew :app:installSecureStorageDebug`
    - Package: `dev.jamescullimore.android_security_training.secure`
  - Vulnerable:
    - Android Studio: Build Variant `vulnStorageDebug`
    - CLI: `./gradlew :app:installVulnStorageDebug`
    - Package: `dev.jamescullimore.android_security_training.vuln`

  B) Preferences demo
  - In the app, click:
    - "Save Token (EncryptedSharedPreferences)" then "Load Token (Encrypted)" → value is stored securely; loaded value is partially redacted.
    - "Save Token (Plain SharedPreferences)" then "Load Token (Plain)" → plaintext storage for contrast.
  - Inspect on device/emulator (debug builds allow run-as):
    ```
    # Secure EncryptedSharedPreferences (ciphertext)
    adb shell run-as dev.jamescullimore.android_security_training.secure ls files/ shared_prefs/
    adb shell run-as dev.jamescullimore.android_security_training.secure cat shared_prefs/secure_prefs.xml  # keys/values appear encrypted

    # Insecure plaintext SharedPreferences
    adb shell run-as dev.jamescullimore.android_security_training.vuln cat shared_prefs/insecure_prefs.xml
    ```

  C) Files demo
  - Click "Write Secure File (EncryptedFile)" then locate file path in the UI output.
  - Also write the insecure file and read it back.
  - Inspect:
    ```
    adb shell run-as dev.jamescullimore.android_security_training.secure ls files/ && hexdump -C files/secure.txt | head
    adb shell run-as dev.jamescullimore.android_security_training.vuln cat cache/insecure.txt
    ```

  D) SQLite demo (plaintext DB for illustration)
  - Click DB Put/Get/List/Delete to manipulate a small table.
  - Inspect with sqlite3 (emulator has it in platform-tools images; if missing, pull the DB):
    ```
    adb shell run-as dev.jamescullimore.android_security_training.secure sqlite3 databases/tokens.db '.tables'
    adb shell run-as dev.jamescullimore.android_security_training.secure sqlite3 databases/tokens.db 'select * from tokens;'
    ```
  - If you pull the DB to your host, open it with a desktop viewer such as DB Browser for SQLite, or use the sqlite3 CLI from https://www.sqlite.org/download.html.

  E) Root awareness in storage demo
  - Secure build guards certain write actions behind a root check (uses Root helper). On rooted devices the action returns a warning instead of writing secrets.

  F) Tips
  - Android Studio → Device Explorer lets you browse app data for debug builds.
  - If run-as fails, ensure you are using a debug build with matching signature and that the app was launched once.

#### Best practices
  - Use EncryptedFile/EncryptedSharedPreferences; prefer Keystore‑backed keys.
  - Never store plaintext credentials, tokens, or PII in world‑readable locations.
  - Apply least privilege file modes and avoid legacy MODE_WORLD_*.
#### Extra reading
  - Jetpack Security library: https://developer.android.com/topic/security/data
  - Android Keystore: https://developer.android.com/training/articles/keystore
  - Scoped storage: https://developer.android.com/about/versions/11/privacy/storage
  - MASVS‑STORAGE: https://mas.owasp.org/MASVS/
  - https://rtx.meta.security/exploitation/2024/03/04/Android-run-as-forgery.html

### 7. Root/Jailbreak detection

#### Where in code
  - Activity UI: `app/src/root/java/.../RootActivity.kt`
  - Secure helper: `app/src/secure/java/.../root/SecureRootHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../root/VulnRootHelper.kt`
#### Lab guide (hands-on)
  Goal: Observe root signals and compare secure vs vuln behavior, including a bypass toggle in vuln.

  A) Build and install variants
  - Secure:
    - Android Studio: Build Variant `secureRootDebug`
    - CLI: `./gradlew :app:installSecureRootDebug`
    - Package: `dev.jamescullimore.android_security_training.secure`
  - Vulnerable:
    - Android Studio: Build Variant `vulnRootDebug`
    - CLI: `./gradlew :app:installVulnRootDebug`
    - Package: `dev.jamescullimore.android_security_training.vuln`

  B) Run checks
  - Tap "Run Root Checks" to list signals (su paths, build tags, known packages, mounts, etc.).
  - Tap "Simulate Block if Rooted" to see policy behavior.
  - Tap "Toggle Bypass (vuln)" to simulate an insecure override (has effect only in vuln builds).
  - Tap "Tamper Check" and "Play Integrity Status (placeholder)" to discuss attestation flows.

  C) Try on a rooted emulator/device (optional)
  - Example commands to surface signals:
    ```
    adb shell su -c 'id'
    adb shell getenforce
    adb shell mount | head -n 20
    ```
  - Expected: Secure build should report rooted=true on common signals; vulnerable build may allow bypass.

  D) Discuss limitations
  - Local checks are heuristics and can be bypassed (MagiskHide/LSPosed/Frida).
  - Combine with server-side attestation (Play Integrity) and degrade gracefully.

E) Emulator note: Google APIs images vs real root
- Many Google APIs emulator images can run adbd as root (so `adb shell su 0 id` works), but do not grant app processes permission to execute `su`.
- Symptoms you may see in this lab:
  - `which su` in `adb shell` returns a path (e.g., `/system/xbin/su`), but the app’s `can execute su` signal fails with "permission denied".
  - `su binary present` and `su in PATH` show true, yet elevation attempts fail.
- Why: The emulator’s root is limited to the adb daemon; normal app UIDs cannot spawn a root shell. This is different from a truly rooted environment (e.g., Magisk/KernelSU), where a superuser manager can grant per-app root.
- What to do for the demo:
  1) Use a rooted image that supports app-level su (e.g., Magisk/KernelSU rooted emulator, Genymotion rooted, or a rooted physical device).
  2) If using Magisk/KernelSU, open the manager app and explicitly allow root for the app package (`dev.jamescullimore.android_security_training.secure` or `.vuln`).
  3) On standard Google APIs emulators, expect signals indicating an emulator and possibly "adbd root only"; use the lab to discuss limitations.

#### Best practices
  - Treat root detection as a risk signal, not a silver bullet; combine with server‑side checks.
  - Fail safely (e.g., reduce functionality) and avoid overly brittle heuristics.
  - Don’t block developer/userdebug builds in internal testing environments without escape hatches.
#### Extra reading
  - https://www.indusface.com/learning/how-to-implement-root-detection-in-android-applications/
  - Android security overview: https://developer.android.com/privacy-and-security
  - Play Integrity API: https://developer.android.com/google/play/integrity
  - SafetyNet Attestation (legacy): https://developer.android.com/training/safetynet/attestation
  - MASVS‑RESILIENCE: https://mas.owasp.org/MASVS/
  - https://grapheneos.org/articles/attestation-compatibility-guide
  - https://developer.android.com/training/articles/security-key-attestation

### 8. WebView & exported components
#### Where in code
  - Topic activity: `app/src/web/java/.../WebActivity.kt`
  - Secure helper/receiver: `app/src/secure/java/.../web/SecureWebViewHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../web/VulnWebViewHelper.kt`
  - Demo ContentProvider (class): `app/src/main/java/.../perm/DemoProvider.kt` (authority overridden in vuln manifest)
  - Vulnerable manifest override: `app/src/vuln/AndroidManifest.xml` (exported provider authority `com.example.demo.provider`)

#### Lab guide (hands-on)
A) Exported ContentProvider attack (adb)
- Build and run `vulnPermDebug` or any vuln topic (provider is registered in the vuln manifest).
- Query the exported provider from the shell:
  ```
  adb shell content query --uri content://dev.jamescullimore.android_security_training.vuln.demo/users
  ```
  Expected (vuln): a row like `hello from DemoProvider: /users` because the provider is exported with no permission checks.

- Compare with secure builds: the same provider is non-exported and gated by a signature permission; external queries should fail.

B) WebView path traversal from file scheme (vuln)
- Build and run `vulnWebDebug` and open the WebView screen.
- Tap "Configure WebView" (enables JS, file://, mixed content, etc. in vuln).
- Tap "Load Untrusted HTTP (cleartext)" to demonstrate mixed content and lack of validation (loads http://neverssl.com/).
- Tap "Load Untrusted FILE (path traversal)" to execute a traversal load. The vuln helper prepares a secret at:
  - `/data/data/<pkg>/files/secret.txt` (created on first run)
- It then calls:
  ```
  webView.loadUrl("file:///android_asset/../../data/data/<pkg>/files/secret.txt")
  ```
  Notes:
  - Modern WebView implementations may block this traversal, but the code and attempt are visible for discussion and testing on older images.
  - Replace `<pkg>` with the running package, e.g., `dev.jamescullimore.android_security_training.vuln`.

C) Resetting app data via broadcast (lab helper)
For quick lab resets, the app includes a BroadcastReceiver that can clear local data.
- Action: `dev.jamescullimore.android_security_training.ACTION_CLEAR_DATA`
- Extras:
  - `what` (optional): one of `prefs`, `files`, `cache`, `db`/`databases`, or omit for `all`.

Usage examples (vulnerable flavors expose this receiver; secure flavors keep it non-exported and gated by a signature permission):
- Vulnerable build (any topic, package suffix `.vuln`):
  ```
  # Clear everything (prefs, files, cache, databases)
  adb shell am broadcast -a dev.jamescullimore.android_security_training.ACTION_CLEAR_DATA -n dev.jamescullimore.android_security_training.vuln/dev.jamescullimore.android_security_training.ClearDataReceiver

  # Clear only SharedPreferences
  adb shell am broadcast -a dev.jamescullimore.android_security_training.ACTION_CLEAR_DATA --es what prefs -n dev.jamescullimore.android_security_training.vuln/dev.jamescullimore.android_security_training.ClearDataReceiver
  ```
- Secure build: The receiver is not exported and requires the custom signature permission, so external adb broadcasts will be ignored/denied by design. Triggering is possible only from inside the app or a same-signature test app.

D) Other vuln WebView demos
- Run JS demo to exfiltrate a token from `addJavascriptInterface` and observe the broadcast.
- Expose/trigger a mutable PendingIntent via broadcast leak.

#### Best practices
  - Disable JS, file access, and mixed content by default.
  - Use a safe URL loading policy and validate origins.
  - Don’t expose WebView JS interfaces to untrusted content; prefer postMessage‑style bridges with strict validation.
  - Avoid exporting components unless required; protect with signature permissions when needed.

#### Extra reading
  - WebView security tips: https://developer.android.com/guide/webapps/webview#security
  - Avoiding intent/component leaks: https://developer.android.com/guide/components/intents-filters#Security
  - Network security config (mixed content): https://developer.android.com/training/articles/security-config#CleartextTrafficPermitted
  - MASVS‑PLATFORM: https://mas.owasp.org/MASVS/

### 9. Multi‑user/AAOS considerations
#### Where in code
  - Topic activity: `app/src/users/java/.../MultiUserActivity.kt`
  - Secure helper: `app/src/secure/java/.../multiuser/SecureMultiUserHelper.kt`
#### Lab guide
  Goal: On a rooted device, create a second user, install and run the vulnerable build under that user to create a token file, then switch back to the primary user and attempt a cross‑user read using the app’s root‑only demo helpers.

  Prereqs
  - Use a rooted device.
  - Build the vulnerable users variant: `vulnUsersDebug`.

  A) Build and install vuln variant for the primary user (user 0)
  - Android Studio: Build Variant `vulnUsersDebug` and Run
  - CLI:
    ```
    ./gradlew :app:installVulnUsersDebug
    ```
  - Package name (vuln): `dev.jamescullimore.android_security_training.vuln`
  - Launcher activity: `dev.jamescullimore.android_security_training.MultiUserActivity`

  B) Create a second user (e.g., "LabUser") and note its userId
  ```
  adb root
  adb shell pm create-user "LabUser"
  adb shell pm list users
  ```
  - From the list, note the new `userId` (commonly 10).

  C) Switch to the new user and make the vuln app available there
  ```
  # Replace 10 with your new userId
  adb shell am switch-user 10
  # If the app is already installed for user 0, this makes it available to user 10
  adb shell pm install-existing --user 10 dev.jamescullimore.android_security_training.vuln
  ```
  If `install-existing` isn’t available on your image, install the APK directly for that user:
  ```
  ./gradlew :app:assembleVulnUsersDebug
  adb install --user 10 app/build/outputs/apk/vuln/users/debug/app-vuln-users-debug.apk
  ```

  D) Launch the app under user 10 and create the token file via the UI
  ```
  adb shell am start --user 10 -n dev.jamescullimore.android_security_training.vuln/dev.jamescullimore.android_security_training.MultiUserActivity
  ```
  - In the Users screen, tap:
    - "Save Per‑User Token (secure)" (in the vuln build this intentionally writes a plaintext token to `shared_prefs/tokens_plain.xml`).
    - Optionally, tap "List Users (best‑effort)" to verify the user list (root‑only demo).

  E) Switch back to the primary user (user 0)
  ```
  adb shell am switch-user 0
  ```

  F) Attempt to read user 10’s token file from user 0 using the app
  - Launch the vuln app (user 0) and open the Users screen.
  - In the "Target userId" field, enter the other user’s id (e.g., 10).
  - Tap "Try Cross‑User Read".
  - Expected on a rooted emulator (vuln build): You should see `[VULN][root-only demo]` output with a snippet of `/data/user/10/…/shared_prefs/tokens_plain.xml` if present. If not found, the app will report a clear not‑found message.

  Notes
  - These cross‑user operations use `su` and are intentionally provided only in the vulnerable flavor for teaching. The secure flavor shows the correct denial/limitations.
  - On some images, you may need to manually interact with the app under the secondary user once before the prefs path exists.

#### Best practices
  - Assume shared devices and multiple profiles (work, guest, automotive) — scope data to the active user.
  - Avoid cross‑profile leaks; respect enterprise restrictions and user separation.
  - Test on AAOS/automotive images where applicable.
#### Extra reading
  - Multi‑user support: https://developer.android.com/reference/android/os/UserManager
  - Android Automotive OS docs: https://developer.android.com/automotive
  - Work profile (Android Enterprise): https://developer.android.com/work
  - MASVS‑ARCH: https://mas.owasp.org/MASVS/

### 10. Risk modeling & dangerous defaults
#### Where in code
Topic `risks` demonstrates configurations that often introduce risk (e.g., allowing cleartext, trusting user CAs, broad intent filters).
#### Lab guide
  1) Run vuln and list “dangerous defaults” that compile but weaken security.
  2) Run secure and compare the stricter defaults and guardrails.
#### Best practices
  - Deny by default: no cleartext, minimal permissions, unexported components.
  - Log security‑relevant events; fail closed when verifications fail.
  - Continuously test with linters, security tests, and CI policies.
#### Extra reading
  - Android app security best practices: https://developer.android.com/privacy-and-security
  - Security checklist: https://developer.android.com/topic/security/best-practices
  - OWASP MASVS (all categories): https://mas.owasp.org/MASVS/
  - Android secure coding (codelabs/guides): https://developer.android.com/courses/pathways/secure-and-private-by-design

## Getting a rooted emulator (for certain labs)
To enable root access: Pick an emulator system image that is NOT labelled "Google Play". (The label text and other UI details vary by Android Studio version.)

Exception: As of 2020-10-08, the Release R "Android TV" system image will not run as root. Workaround: Use the Release Q (API level 29) Android TV system image instead.

Test it: Launch the emulator, then run `adb root`.

Command:
```
adb root
```
Expected output:
```
restarting adbd as root
```
or
```
adbd is already running as root
```
Not acceptable:
```
adbd cannot run as root in production builds
```

Alternate test:
```
adb shell
$ su
#
```
If the shell shows `#` after `su`, you have root. If it stays `$`, root is not available.

Steps: To install and use an emulator image that can run as root:

- In Android Studio, use the menu command Tools > AVD Manager.
- Click the + Create Virtual Device... button.
- Select the virtual Hardware, and click Next.
- Select a System Image.
- Pick any image that does NOT say "(Google Play)" in the Target column.
- If you depend on Google APIs (Google Sign In, Google Fit, etc.), pick an image marked with "(Google APIs)".
- You might have to switch from the "Recommended" group to the "x86 Images" or "Other Images" group to find one.
- Click the Download button if needed.
- Finish creating your new AVD.
- Tip: Start the AVD Name with the API level number so the list of Virtual Devices will sort by API level.
- Launch your new AVD. (You can click the green "play" triangle in the AVD window.)

## Frida

- Install Frida: https://frida.re/docs/installation/
- Set up for Android (device/emulator and frida-server): https://frida.re/docs/android/
- Example scripts for this lab are in the `frida/` folder of this repo.

### Quick start (attach/spawn)
- List device processes (verify connection):
  ```
  frida-ps -U
  ```
- Spawn the secure build with a script (example: hook secure storage):
  ```
  frida -U -f dev.jamescullimore.android_security_training.secure -l frida/hook_secure_storage.js
  ```
  - Tip: add `--no-pause` to let the app run immediately after spawn.
- Attach to a running app instead of spawning (alternative):
  ```
  frida -U -n dev.jamescullimore.android_security_training.secure -l frida/hook_secure_storage.js
  ```
- Trace common libc calls (example: `open`) for the secure build:
  ```
  frida-trace -U -i open -N dev.jamescullimore.android_security_training.secure
  ```
  - Alternate syntax (attach by name on some versions):
    ```
    frida-trace -U -i open -n dev.jamescullimore.android_security_training.secure
    ```

### Notes
- Package IDs:
  - Secure variants: `dev.jamescullimore.android_security_training.secure`
  - Vulnerable variants: `dev.jamescullimore.android_security_training.vuln`
- Scripts live in the `frida/` directory (for example: `frida/hook_secure_storage.js`).
- If you don’t see classes/methods right after spawning, use `--no-pause` or interact with the target screen so code paths load.
- On Google APIs emulators, Frida works fine for app‑level hooking even if `adbd` runs as root but the app cannot `su` — this is expected (see Root section for details).

## Troubleshooting
- Build with the Gradle wrapper from Android Studio. If secure pinning fails, check device time and that pins match the current server keys.
- Deep links require a matching `assetlinks.json` on your domain. Update host/path if you change it.
- For MITM demos, remember: secure flavors do not trust user CAs; use vuln flavors.
- If the emulator won’t run as root, confirm you didn’t pick a Google Play image (see section above).
- Expect‑CT (legacy): https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT
