# Android Security Training

Teach Android security with hands‑on, side‑by‑side examples. Each topic has two flavors: a secure implementation that follows best practices and a deliberately vulnerable one for learning and demos.

This README is concise but complete: purpose, prerequisites, build variants, how to run each of the 10 topics, best‑practice notes, rooted emulator tips, and further reading.

## Table of contents
- [Purpose and scope](#purpose-and-scope)
- [Prerequisites (tools) and before‑you‑start](#prerequisites-tools-and-before-you-start-steps)
- [Quick start](#quick-start)
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
  - [9. Multi‑user/AAOS considerations](#9-multi-useraaos-considerations)
  - [10. Risk modeling & dangerous defaults](#10-risk-modeling--dangerous-defaults)
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
- Optional but recommended for network labs:
  - mitmproxy, Burp Suite, or OWASP ZAP
  - Wireshark or tcpdump
- Before you start:
  1) Clone this repo and open it in Android Studio.
  2) Let Gradle sync and index completely.
  3) Decide which topic you want to run first (see Build variants), then pick a secure or vuln profile.
  4) If you plan to demo MITM, configure your proxy and device/emulator networking first.

## Quick start
1) Open the project in Android Studio (latest stable).
2) In Build Variants, choose a pair like `clientVulnPinning` (to see the issue) then `clientSecurePinning` (to see the fix).
3) Run on an emulator or device and follow the on‑screen actions for the selected topic.
4) For network labs, configure your proxy before launching the vulnerable build.

## Build variants (how this project is organized)
- Two flavor dimensions in `app/build.gradle.kts`:
  - securityProfile: `secure` (best practices) or `vuln` (intentionally unsafe). Release builds are disabled for `vuln`.
  - topic: `pinning`, `e2e`, `re`, `perm`, `links`, `storage`, `root`, `web`, `users`, `risks`.
- The final build variant is `<securityProfile><Topic>`, for example:
  - `clientSecurePinning`, `clientVulnPinning`
  - `clientSecureWeb`, `clientVulnWeb`, etc.
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
  1) Build `clientVulnPinning` and route traffic through your proxy. Observe successful MITM using a user‑installed CA and, optionally, cleartext.
  2) Build `clientSecurePinning` and repeat. Requests should fail when intercepted or when certificates don’t match pins.
  3) Try rotating the server certificate to demonstrate pin failures.
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

### 2. End‑to‑end encryption (E2E)
#### Where in code
  - API surface: `app/src/main/java/.../crypto/CryptoHelper.kt`
  - Secure: `app/src/secure/java/.../crypto/SecureCryptoHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../crypto/VulnCryptoHelper.kt`
#### Lab guide
  1) Run the secure variant and send an encrypted payload to a demo endpoint.
  2) Compare with the vuln variant (e.g., ECB/static key or no integrity).
  3) Modify inputs and show how tampering is detected only with AEAD (GCM/ChaCha20‑Poly1305).
#### Best practices
  - Use modern AEAD (AES‑GCM or ChaCha20‑Poly1305) with random nonces and include AAD where relevant.
  - Derive keys via a KDF and rotate regularly; never hardcode keys.
  - Use the Android Keystore for long‑term keys; avoid exporting private keys.
#### Extra reading
  - Android Keystore: https://developer.android.com/training/articles/keystore
  - Cryptography best practices (Android): https://developer.android.com/privacy-and-security/crypto
  - OWASP MASVS‑CRYPTO: https://mas.owasp.org/MASVS/
  - NIST SP 800‑38D (GCM): https://csrc.nist.gov/publications/detail/sp/800-38d/final

### 3. Reverse‑engineering resistance
#### Where in code
  - Secure helper: `app/src/secure/java/.../re/SecureReDemoHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../re/VulnReDemoHelper.kt`
#### Lab guide
  1) Run vuln and inspect the APK (strings/resources can reveal secrets). Try patching/smali modifications.
  2) Run secure and observe checks (e.g., signature digest) and reduced exposed data.
#### Best practices
  - Don’t store secrets in the APK; prefer server‑issued, short‑lived tokens.
  - Enable R8/ProGuard shrinking/obfuscation for release; strip debug info from release.
  - Verify app signature at runtime for critical logic paths; use SafetyNet/Play Integrity as an additional signal when appropriate.
#### Extra reading
  - App signing & verifying: https://developer.android.com/studio/publish/app-signing
  - Code shrinking, obfuscation, optimization: https://developer.android.com/studio/build/shrink-code
  - OWASP MASVS‑RESILIENCE: https://mas.owasp.org/MASVS/
  - Android app reverse engineering overview (docs): https://developer.android.com/privacy-and-security

### 4. Runtime permissions
#### Where in code
`app/src/perm/java/...`
#### Lab guide
  1) In vuln, request broad or unnecessary permissions and demonstrate data access.
  2) In secure, request only when needed and show graceful denial handling.
#### Best practices
  - Request the minimum set, at time‑of‑use; provide clear rationale.
  - Handle denial and “don’t ask again” states; offer in‑app settings navigation.
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
#### Lab guide
  1) In vuln, register overly broad intent filters; demonstrate hijack/phishing flows.
  2) In secure, use verified app links for your domain and validate parameters/paths.
#### Best practices
  - Prefer verified app links (assetlinks.json) for https domains.
  - Validate schemes, hosts, and path prefixes explicitly; reject unexpected ones.
  - Avoid exporting components unless required; use `android:exported="false"` by default.
#### Extra reading
  - Verify Android App Links: https://developer.android.com/training/app-links/verify-site-associations
  - Deep links documentation: https://developer.android.com/training/app-links/deep-linking
  - Digital Asset Links: https://developer.android.com/training/app-links/associate-website
  - MASVS‑PLATFORM: https://mas.owasp.org/MASVS/

### 6. Secure storage
#### Where in code
  - Secure: `app/src/secure/java/.../storage/SecureStorageHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../storage/VulnStorageHelper.kt`
#### Lab guide
  1) In vuln, write sensitive data to unencrypted/shared storage and read it from adb/files.
  2) In secure, store secrets in Keystore or EncryptedSharedPreferences and demonstrate protected access.
#### Best practices
  - Use EncryptedFile/EncryptedSharedPreferences; prefer Keystore‑backed keys.
  - Never store plaintext credentials, tokens, or PII in world‑readable locations.
  - Apply least privilege file modes and avoid legacy MODE_WORLD_*.
#### Extra reading
  - Jetpack Security library: https://developer.android.com/topic/security/data
  - Android Keystore: https://developer.android.com/training/articles/keystore
  - Scoped storage: https://developer.android.com/about/versions/11/privacy/storage
  - MASVS‑STORAGE: https://mas.owasp.org/MASVS/

### 7. Root/Jailbreak detection
#### Where in code
  - Secure: `app/src/secure/java/.../root/SecureRootHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../root/VulnRootHelper.kt`
#### Lab guide
  1) Use a rooted emulator/device and compare signals (su paths, Magisk traces, SELinux) between secure and vuln.
  2) Show how root can weaken other defenses (e.g., user CA trust on some builds).
#### Best practices
  - Treat root detection as a risk signal, not a silver bullet; combine with server‑side checks.
  - Fail safely (e.g., reduce functionality) and avoid overly brittle heuristics.
  - Don’t block developer/userdebug builds in internal testing environments without escape hatches.
#### Extra reading
  - Android security overview: https://developer.android.com/privacy-and-security
  - Play Integrity API: https://developer.android.com/google/play/integrity
  - SafetyNet Attestation (legacy): https://developer.android.com/training/safetynet/attestation
  - MASVS‑RESILIENCE: https://mas.owasp.org/MASVS/

### 8. WebView & exported components
#### Where in code
  - Topic activity: `app/src/web/java/.../WebActivity.kt`
  - Secure helper/receiver: `app/src/secure/java/.../web/SecureWebViewHelper.kt`
  - Vulnerable helper: `app/src/vuln/java/.../web/VulnWebViewHelper.kt`
#### Lab guide
  1) In vuln, enable JavaScript bridges and mixed content; exploit token leakage or XSS navigation.
  2) In secure, enforce strict allowlists, disable JS unless required, and keep components non‑exported.
#### Best practices
  - Disable JS, file access, and mixed content by default.
  - Use a safe URL loading policy and validate origins.
  - Don’t expose WebView JS interfaces to untrusted content; prefer postMessage‑style bridges with strict validation.
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
  1) Explore how user profiles affect storage and component visibility.
  2) Compare secure vs vuln behavior around user isolation and exported components.
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

## Troubleshooting
- Build with the Gradle wrapper from Android Studio. If secure pinning fails, check device time and that pins match the current server keys.
- Deep links require a matching `assetlinks.json` on your domain. Update host/path if you change it.
- For MITM demos, remember: secure flavors do not trust user CAs; use vuln flavors.
- If the emulator won’t run as root, confirm you didn’t pick a Google Play image (see section above).
- Expect‑CT (legacy): https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT