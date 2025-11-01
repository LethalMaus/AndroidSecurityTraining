# Android Security Training

A hands-on training app showing secure vs vulnerable implementations across common Android security topics. This README is clean, concise, and maps each topic to its screen and code so you can get productive fast.

Table of contents
- Overview
- Build variants and topics
- Quick start
- Topics at a glance (where to look in code)
- Configuration defaults (current values)
- Running the labs
- Troubleshooting
- References

Overview
- Two security profiles via flavors: secure (best practices) and vuln (intentionally unsafe for demos).
- Multiple topic flavors so you can focus a build on one subject (pinning, e2e, re, perm, links, storage, root, web, users, risks).
- Everything compiles out of the box; no placeholders are left in code.

Build variants and topics
- Flavor dimensions: securityProfile ∈ {secure, vuln}, topic ∈ {pinning, e2e, re, perm, links, storage, root, web, users, risks}.
- Examples:
  - clientSecurePinning → secure profile + pinning topic
  - clientVulnWeb → vulnerable profile + web topic
- Providers route to the correct helper per profile. In secure builds, a BuildConfig flag (MANUAL_PIN=false by default) can route the pinning demo through a manual TrustManager.

Quick start
1) Open the project in Android Studio (latest stable).
2) Select a build variant that matches your session, e.g. clientSecurePinning or clientVulnE2e.
3) Run on an emulator/device. Each topic screen has actions and on-screen notes.

Topics at a glance (where to look)
- Certificate Pinning & HTTPS
  - Interface: app/src/main/java/.../network/NetworkHelper.kt
  - Secure: app/src/secure/java/.../network/SecureNetworkHelper.kt
  - Vulnerable: app/src/vuln/java/.../network/VulnNetworkHelper.kt
  - Provider (secure): app/src/secure/java/.../network/Provider.kt
  - Config (secure): app/src/secure/res/xml/network_security_config_client_secure.xml
- End-to-End Encryption (E2E)
  - UI: app/src/e2e/java/.../E2EActivity.kt
  - API: app/src/main/java/.../crypto/CryptoHelper.kt
  - Secure: app/src/secure/java/.../crypto/SecureCryptoHelper.kt
  - Vulnerable: app/src/vuln/java/.../crypto/VulnCryptoHelper.kt
- Deep Links
  - UI: app/src/links/java/.../DeepLinksActivity.kt
  - Secure helper: app/src/secure/java/.../deeplink/SecureDeepLinkHelper.kt
  - Manifest (links topic): app/src/links/AndroidManifest.xml (verified app links)
- Storage
  - UI: app/src/storage/java/.../StorageActivity.kt
  - Secure helper: app/src/secure/java/.../storage/SecureStorageHelper.kt
  - Vulnerable helper: app/src/vuln/java/.../storage/VulnStorageHelper.kt
- Reverse Engineering (RE)
  - UI: app/src/re/java/.../REActivity.kt
  - Secure helper: app/src/secure/java/.../re/SecureReDemoHelper.kt
  - Vulnerable helper: app/src/vuln/java/.../re/VulnReDemoHelper.kt
- Root Detection
  - UI: app/src/root/java/.../RootActivity.kt
  - Secure helper: app/src/secure/java/.../root/SecureRootHelper.kt
  - Vulnerable helper: app/src/vuln/java/.../root/VulnRootHelper.kt
- WebView & Exposed Components
  - UI: app/src/web/java/.../WebActivity.kt
  - Secure helper/receiver: app/src/secure/java/.../web/SecureWebViewHelper.kt (+ DemoReceiver in main)
  - Vulnerable helper: app/src/vuln/java/.../web/VulnWebViewHelper.kt
- Multi‑user (AAOS and multi-user devices)
  - UI: app/src/users/java/.../MultiUserActivity.kt
  - Secure helper: app/src/secure/java/.../multiuser/SecureMultiUserHelper.kt
  - Vulnerable helper: app/src/vuln/java/.../multiuser/VulnMultiUserHelper.kt

Configuration defaults (current values)
- NetworkHelper.DEFAULT_URL: https://api.github.com/
- Secure pinning domain: api.github.com
  - SPKI pins (SHA‑256, Base64):
    - 1EkvzibgiE3k+xdsv+7UU5vhV8kdFCQiUiFdMX5Guuk=
    - fXkqYy8jL6cDXcYJvLgk0i8V0CVg28t3Tw4eBeaHeoA=
  - Pin-set expiration (network security config): 2026‑10‑21
- E2E "Encrypt + Send" endpoint: https://postman-echo.com/post
- ECDH demo server key: PEM public key embedded in E2EActivity (P‑256) — replace as needed.
- Verified app links (links topic):
  - Host: lethalmaus.github.io
  - Path prefix: /AndroidSecurityTraining
- Secure signer (tamper check) digest (Base64, SHA‑256):
  - Plazc2oWHYXXVf8ZXUPiLS9fBySu3GhTc0qg/fTy+/I=
- Manual pin routing flag (secure profile): BuildConfig.MANUAL_PIN=false (can be overridden per build).

Running the labs
- Pinning/HTTPS
  - Secure: disallows cleartext, trusts system CAs only, enforces pins via Network Security Config and OkHttp.
  - Vulnerable: allows cleartext and user CAs and may relax hostname verification for demonstration.
- E2E
  - Secure: AES‑GCM with random IV and tag; optional AAD; demo ECDH derives a session key.
  - Vulnerable: ECB with static key; Base64 treated as "encryption"; no integrity.
- Deep links
  - Secure: validates https scheme, host and path prefix; requires state parameter for example OAuth callback flow.
  - Vulnerable: accepts broad schemes/hosts with minimal validation (for classroom exploit demos).
- WebView & components
  - Secure: JS disabled by default; strict URL allowlist; no mixed content; receivers not exported.
  - Vulnerable: JS bridge leaks a token; mixed content allowed; exported receiver demonstrates broadcast abuse.
- Reverse engineering
  - Compare SecureReDemoHelper vs VulnReDemoHelper; in secure builds, signature digest is enforced at runtime.
- Root
  - Secure helper aggregates common signals (su paths, Magisk traces, SELinux, mounts) and checks signer digest; vuln returns permissive values.

Troubleshooting
- Build succeeds with Gradle wrapper; if you see connectivity failures in secure builds, verify your device time and that api.github.com pins are still valid.
- Deep links require a matching assetlinks.json hosted for lethalmaus.github.io; update host/path if you change domains.
- If you test MITM, remember secure flavors do not trust user CAs; use vuln flavors for interception demos.

References
- Android Network Security Config: https://developer.android.com/training/articles/security-config
- OkHttp CertificatePinner: https://square.github.io/okhttp/features/certificates/#certificate-pinner
- Certificate Transparency (overview): https://certificate.transparency.dev/
- Expect‑CT (legacy): https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT

---
Legacy appendix follows (original long-form README kept below for reference).

To add real CT verification in the app (optional, recommended for training environment):
- Consider integrating Google’s Certificate Transparency Android library: com.google.certificate-transparency:certificate-transparency-android (version depends on your setup) 
- Or a similar library that can parse and verify SCTs from the TLS handshake when supported by the platform provider (e.g., Conscrypt) 
- Wire the verifier as a TrustManager wrapper or an OkHttp event listener; fail the connection when SCTs are missing or invalid based on your policy 

References and guides:
- Android Network Security Config: https://developer.android.com/training/articles/security-config
- OkHttp CertificatePinner: https://square.github.io/okhttp/features/certificates/#certificate-pinner
- Certificate Transparency (overview): https://certificate.transparency.dev/
- Expect-CT (legacy header): https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT


## Real-World Attacks and Failures (for classroom exercises)

Vulnerabilities demonstrated in vuln flavors:
- Trusting user-installed CAs: Allows on-device MITM with a locally installed root CA
- Allowing cleartext: Sensitive data exposure to passive/active attackers on the network
- Hostname verification disabled: Enables cert-for-any-host attacks

Classroom exploitation ideas:
- MITM with a user CA on a rooted device/emulator (or user-added CA on userdebug):
    1) Build and install clientVuln or autoVuln
    2) Install your training root CA on the device (user store) — device-specific steps
    3) Run a proxy (Burp/ZAP/mitmproxy) and point device traffic through it (adb reverse/port proxy or Wi‑Fi proxy)
    4) Observe intercepted TLS sessions succeeding because the app trusts the user CA

- Cleartext sniffing:
    1) Use clientVuln/autoVuln
    2) Point DEFAULT_URL to http://<your-lab-host>:<port> 
    3) Capture with Wireshark and show credentials in plaintext

- Pinning failures:
    1) Use clientSecure/autoSecure with real pins configured
    2) Rotate server cert to one not matching the pin
    3) Observe connection failures and app behavior

- CT policy discussion:
    - Discuss how lack of CT (or invalid SCTs) could allow misissued certs to go undetected, and how monitors/logs detect and surface them.


## Where to Insert Your Training Infrastructure

- Replace NetworkHelper.DEFAULT_URL with your server endpoint 
- Add real certificate pins in:
    - src/clientSecure/res/xml/network_security_config_client_secure.xml
    - src/autoSecure/res/xml/network_security_config_auto_secure.xml
    - SecureNetworkHelper CertificatePinner builder (src/secure/...)
- If adding a CT verifier, add the library dependency and wire a validating TrustManager 


## Source Layout

- app/src/main/java/.../MainActivity.kt — Compose UI with a button to run the demo request
- app/src/main/java/.../network/NetworkHelper.kt — interface and DEFAULT_URL
- app/src/secure/java/.../network/SecureNetworkHelper.kt — secure implementation with pinning and CT logging
- app/src/secure/java/.../network/Provider.kt — provideNetworkHelper() for secure flavors
- app/src/vuln/java/.../network/VulnNetworkHelper.kt — intentionally vulnerable implementation
- app/src/vuln/java/.../network/Provider.kt — provideNetworkHelper() for vuln flavors
- Per-flavor manifests and network security configs under app/src/<flavor>/


## AAOS Notes

- AAOS builds share the same code but use distinct network security configs.
- OEMs often control trust stores and policies; system cert store may differ from handheld devices.
- Some head units lack convenient UIs for managing user CAs; secure profiles avoid user CAs entirely.


## Tooling Exercises

- Wireshark: capture cleartext in vuln flavors; inspect TLS handshakes and observe SCT/OCSP stapling
- JADX/apktool: reverse the APK; locate the vulnerable trust manager and hostname verifier in the vuln build
- MobSF: run a static scan and review findings (e.g., cleartextTrafficPermitted)


## Caveats

- This sample is for training only. Do not ship vulnerable configurations.
- Placeholder URLs and pins must be replaced with your lab infrastructure. Look for  markers in code and XML.


## Section 2: Encrypting Data Before Transport (End-to-End Encryption)

This topic demonstrates encrypting sensitive payloads at the application layer before sending them over HTTPS. It covers end-to-end principles, symmetric vs asymmetric options, common pitfalls (encoding vs encryption), and integration with HTTPS. Each security profile (secure vs vuln) shows a contrasting implementation.

How to build and run:
- Select a variant that includes the e2e topic, e.g., clientSecureE2e, clientVulnE2e, autoSecureE2e, autoVulnE2e.
- Launch the app. You’ll see the E2E screen with actions:
    - Encrypt Locally (AES-GCM)
    - Encrypt + Send via HTTPS
    - ECDH Derive Session Key (Demo)
    - Encoding vs Encryption (Base64)

Key code paths:
- UI (topic-specific): app/src/e2e/java/.../E2EActivity.kt
- Crypto API (interface): app/src/main/java/.../crypto/CryptoHelper.kt
- Secure crypto implementation: app/src/secure/java/.../crypto/SecureCryptoHelper.kt
    - AES-GCM with random IV, 128-bit tag
    - HMAC-SHA256 example
    - ECDH key agreement (P-256) to derive a session key (demo only)
    - HTTPS integration: posts encrypted JSON to https://httpbin.org/post (for echo).  set your seminar backend URL that can decrypt
- Vulnerable crypto implementation: app/src/vuln/java/.../crypto/VulnCryptoHelper.kt
    - AES/ECB with static key (pattern leakage, trivial to extract)
    - Misuse of Base64 as “encryption”
    - No integrity protection, permissive hostname verification

Example encrypted JSON shape (secure build):
{
"alg": "AES-GCM",
"iv": base64,
"tag": base64,
"aad": base64,
"ciphertext": base64
}

Notes on key management and exchange:
- Symmetric keys: In this demo, keys are in-memory for simplicity. In real apps, use Android Keystore (Hardware-backed when available) and scoped keys per purpose (encryption vs MAC).
- Key exchange: Secure build shows ECDH (P-256) with a placeholder server public key (PEM). Replace with your server’s real key. 
- Derivation: This sample uses PBKDF2 to derive an AES key from the shared secret to avoid external deps. For production, use HKDF with salt and context info.

Best practices (Android):
- Prefer AES-GCM (or ChaCha20-Poly1305 on Conscrypt/BoringSSL stacks) with random 96-bit IVs.
- Always authenticate data (AEAD like GCM already does), and include AAD when binding to context (e.g., API version or request id).
- Separate keys per purpose; do not reuse encryption keys as MAC keys.
- Rotate keys; include key IDs in payloads; plan for key rollovers.
- Store long-term private keys in Android Keystore; require user auth if appropriate (BiometricPrompt + setUserAuthenticationParameters()).
- Never confuse encoding (Base64) with encryption; Base64 is reversible without a key.
- Validate server certs as usual (this is an app-layer encryption on top of TLS, not a replacement for TLS).

Exploitation exercises (vuln builds):
- Pattern leakage with ECB: show that repeating blocks produce repeating ciphertext. Try modifying the JSON to have repeated patterns and observe the ciphertext changes.
- Static key extraction: Use JADX/apktool to find the hard-coded key in VulnCryptoHelper.
- Base64 as “encryption”: Intercept with a proxy and decode Base64 to recover plaintext instantly.
- Hostname bypass (if routed via vuln networking in your setup): demonstrate MITM feasibility.

AAOS vs client differences:
- AAOS devices may have different TLS stacks or policies; the app-layer encryption is portable but ensure performance constraints (head unit CPUs) are respected. Avoid heavy PBKDF2 iteration counts; prefer HKDF.
- UI interactions may be limited; the training demo keeps actions simple for both form factors.

 checklist for your seminar:
- Replace ECDH server public key with your lab’s key in E2EActivity placeholder.
- Replace HTTPS POST URL in SecureCryptoHelper/VulnCryptoHelper with your API endpoint.
- If you want true server-side decryption, implement a small training backend that accepts the JSON envelope, verifies tag/AAD, and decrypts.
- Optionally integrate Android Keystore-backed keys and HKDF utility.



## Exercise: Setting Up Certificate Pins and Monitoring Network Traffic

This exercise is doable with the current codebase. The repo already includes:
- Per-variant Network Security Configs with example pinning (secure flavors) and permissive configs (vuln flavors).
- OkHttp-based client with CertificatePinner in secure builds.
- A vulnerable client that trusts all certs for interception demonstrations.
- NEW: A manual TrustManager-based pinning helper (ManualPinNetworkHelper) for hands-on pinning risks and trade-offs.

What you’ll accomplish:
- Implement SPKI pins via NetworkSecurityConfig and OkHttp CertificatePinner.
- Toggle to a manual TrustManager and see how easy it is to get pinning wrong.
- Intercept non-pinned traffic with mitmproxy/Wireshark and observe failures when pins are active.
- Inspect Certificate Transparency signals and discuss pin rollout strategies.

Preconditions recap:
- Android Studio (latest), adb in PATH
- mitmproxy or Wireshark installed
- Emulator or device with developer options enabled
- Starter project: this repo (choose a pinning topic variant)

Which build variant to choose:
- Secure: clientSecurePinning (pins enforced; good baseline)
- Vulnerable: clientVulnPinning (easy interception)

Where to set the target host and pins:
- URL: NetworkHelper.DEFAULT_URL in app/src/main/java/.../network/NetworkHelper.kt 
- NetworkSecurityConfig pins: secure flavors XML under app/src/*Secure/res/xml/ 
- OkHttp CertificatePinner: app/src/secure/java/.../network/SecureNetworkHelper.kt 
- Manual TrustManager pins: app/src/secure/java/.../network/ManualPinNetworkHelper.kt 

How to switch between the secure approaches:
- Default secure path uses OkHttp CertificatePinner.
- To use the manual TrustManager demo in secure builds, open:
  app/src/secure/java/.../network/Provider.kt
  and set MANUAL_PIN = true (). Rebuild and run. Note: We intentionally keep this toggle simple for classroom use.

Step-by-step lab (60–90 min):
1) Warm-up (5–10 min)
    - Build and run clientVulnPinning.
    - Set device/emulator proxy to mitmproxy (or run adb reverse; see below).
    - Change DEFAULT_URL to an HTTPS site you control or a lab endpoint.
    - Observe interception success due to trusting user CAs and no pins.

2) Add pins with NetworkSecurityConfig (15–20 min)
    - Switch to clientSecurePinning.
    - Compute SPKI pin for your host:
      openssl s_client -connect example.com:443 -servername example.com < /dev/null 2>/dev/null \
      | openssl x509 -pubkey -noout \
      | openssl pkey -pubin -outform DER \
      | openssl dgst -sha256 -binary \
      | openssl enc -base64
    - Insert primary and backup pins into the secure flavor’s network_security_config_*.xml.
    - Run the app and verify success against the real server; confirm mitmproxy now fails.

3) Add OkHttp CertificatePinner (10–15 min)
    - Add the same pins in SecureNetworkHelper’s CertificatePinner.Builder().
    - Discuss overlap vs redundancy between NSC pins and app-layer pins.

4) Try manual TrustManager pinning (15–20 min)
    - Toggle MANUAL_PIN = true in Provider.kt.
    - Update pinnedHost and pins in ManualPinNetworkHelper.kt.
    - Run again. Intentionally break a best practice (e.g., remove backup pin) and note failure modes.
    - Discuss risks of custom trust code and why platform validation must still run.

5) Monitor traffic with Wireshark/mitmproxy (throughout)
    - mitmproxy setup options:
        - On emulator: Settings > Network > Proxy, point to host machine IP:8080.
        - Or adb reverse (for a specific TCP port if you control the server):
          adb reverse tcp:8443 tcp:8443
    - Wireshark: capture on the relevant interface; compare vuln vs secure builds.

6) Certificate Transparency discussion (10–15 min)
    - In secure builds, look for Logcat tag "CT" from interceptors.
    - Use external CT monitors for your domain (examples):
        - crt.sh: https://crt.sh/?q=example.com
        - Google CT: https://transparencyreport.google.com/https/certificates
        - certificate.transparency.dev resources
    - Topics: SCT delivery (embedded, TLS ext, OCSP), misissuance detection, monitors/alerts.

7) Rollout strategies and pin life-cycle (5–10 min)
    - Always ship at least one backup pin.
    - Stage rollouts; observe failure telemetry.
    - Plan for CA rotations/ACME renewals; update pins ahead of expiry.
    - Consider feature flags/remote config to disable pin checks in emergencies (with care).

Homework
- Research advanced pinning strategies and automated updates (e.g., remote-config pinsets, TUF-style signing).
- Read: "HTTPS Certificate Pinning for Android" (Android docs).
- Optional: Implement a pinning failure fallback notification in the app and report the hostname and pinset version.

Quick reference: device proxying for mitmproxy
- Emulator (Android): Settings > Network & Internet > Wi‑Fi > current network > Proxy: Manual
  Host: <your_host>, Port: 8080
- Physical device via Wi‑Fi: similar steps; ensure host is reachable.
- Turn off proxy when testing real pinned connections.

Troubleshooting
- If secure builds connect through mitmproxy, your pins likely aren’t applied or are set for the wrong domain.
- If all builds fail, confirm DEFAULT_URL is reachable and your pins match the live cert’s SPKI.
- For AAOS variants, verify the head unit’s network path and trust store behavior.

# Android Security Training — Sections: (1) Certificate Pinning & Transparency, (2) Encrypting Data Before Transport

This repository contains an example Android app set up for security training exercises. This section focuses on the HTTPS/TLS stack, certificate pinning, dangers of user-installed CAs, handling updates/pin rollouts, and especially Certificate Transparency (CT). Each topic includes both a secure implementation and an intentionally vulnerable one, separated by build variants. Client and Automotive (AAOS) form factors are both represented.

Note: Some values are placeholders with  so you can point the app at your own training infrastructure later.


## Prerequisites and Environment Requirements

- Experience developing on embedded Linux
- Basic understanding of app-level security (TLS, file system, permissions)
- No prior Android experience needed
- Android Studio (latest)
- adb (in PATH)
- Wireshark
- JADX, apktool, MobSF
- Physical or emulator device with Google Play services (rooted and non‑rooted)
- Non-congested Wi‑Fi network with internet access


## Project Overview

This app now uses three flavor dimensions to separate concerns:

- Form factor: client
- Security profile: secure, vuln
- Training topic: pinning, e2e, re

Examples of combined variants (Build Variants in Android Studio):
- clientSecurePinning — Secure client example for Certificate Pinning & CT
- clientVulnPinning — Vulnerable client example for pinning topic
- clientSecureE2e — Secure client example for Encrypting Data Before Transport
- clientVulnE2e — Vulnerable client example for E2E encryption topic
- clientSecureRe — Secure client example for Reverse Engineering topic
- clientVulnRe — Vulnerable client example for Reverse Engineering topic

Key implementation points:
- Network Security Config per variant under src/<flavor>/res/xml/
- OkHttp used for HTTP(S) with logging
- Secure variants use standard TrustManager + hostname verification + optional pinning
- Vulnerable variants use trust-all TrustManager + hostname bypass (for training ONLY)
- CT focus: Interceptor logs SCT/Expect-CT to explain transparency; README includes guidance for full verification


## Building and Running

1) Open in Android Studio (latest).
2) Select Build Variant in the Build Variants tool window:
    - clientSecure, clientVuln, autoSecure, or autoVuln
3) Run on a device or emulator.

Variant availability note (2025-10-24):
- Release build variants for all 'vuln' flavors are disabled. Use Debug for vuln demos.
- Secure flavors retain both Debug and Release variants.

The landing screen shows the active variant (BuildConfig.FLAVOR) and a button “Run Demo Request” that performs an HTTPS request using the variant’s network configuration.

Default URL is https://example.com/ (replace in code with your server). Look for logs tagged "CT" to see transparency-related info.


## Network Security Configs

Located in per-variant XML files:

- clientSecure: src/clientSecure/res/xml/network_security_config_client_secure.xml
    - Disallows user CAs
    - Disallows cleartext
    - Demonstrates certificate pinning with placeholder pins 

- clientVuln: src/clientVuln/res/xml/network_security_config_client_vuln.xml
    - Trusts user CAs (vulnerable)
    - Allows cleartext (vulnerable)

- autoSecure: src/autoSecure/res/xml/network_security_config_auto_secure.xml
    - Disallows user CAs
    - Disallows cleartext
    - Pinning example for AAOS backend with placeholder pins 

- autoVuln: src/autoVuln/res/xml/network_security_config_auto_vuln.xml
    - Trusts user CAs (vulnerable)
    - Allows cleartext (vulnerable)

Note on AAOS: Device policy and certificate stores may be managed by OEMs. For AAOS, pay special attention to system vs user trust anchors; secure profiles explicitly avoid user certs.


## Certificate Pinning

Two places you will see (demonstration) pinning:
- Network Security Config pin-sets in secure flavors
- OkHttp’s CertificatePinner in SecureNetworkHelper (src/secure/...)

Replace the placeholder pins with real SPKI or certificate SHA-256 values.
- How to get a pin using OpenSSL:
  openssl s_client -connect example.com:443 -servername example.com < /dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

Pin rollout considerations (discuss in class):
- Always include at least one backup pin
- Staged rollouts; monitor failures
- Coordinate with certificate rotations (ACME/Let’s Encrypt renewals) to prevent outages


## Certificate Transparency (CT)

Android and Chrome enforce CT policy for many public CAs and high-profile domains, but app-level CT verification isn’t automatic. This app demonstrates how to surface CT-related signals and how to integrate full verification if you choose.

Current demo implementation:
- SecureNetworkHelper adds a network interceptor logging SCT or Expect-CT headers if present.
- Use these logs to discuss SCT delivery (embedded, TLS extension, OCSP stapling) and the purpose of CT logs and monitors.

To add real CT verification in the app (optional, recommended for training environment):
- Consider integrating Google’s Certificate Transparency Android library: com.google.certificate-transparency:certificate-transparency-android (version depends on your setup) 
- Or a similar library that can parse and verify SCTs from the TLS handshake when supported by the platform provider (e.g., Conscrypt) 
- Wire the verifier as a TrustManager wrapper or an OkHttp event listener; fail the connection when SCTs are missing or invalid based on your policy 

References and guides:
- Android Network Security Config: https://developer.android.com/training/articles/security-config
- OkHttp CertificatePinner: https://square.github.io/okhttp/features/certificates/#certificate-pinner
- Certificate Transparency (overview): https://certificate.transparency.dev/
- Expect-CT (legacy header): https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT


## Real-World Attacks and Failures (for classroom exercises)

Vulnerabilities demonstrated in vuln flavors:
- Trusting user-installed CAs: Allows on-device MITM with a locally installed root CA
- Allowing cleartext: Sensitive data exposure to passive/active attackers on the network
- Hostname verification disabled: Enables cert-for-any-host attacks

Classroom exploitation ideas:
- MITM with a user CA on a rooted device/emulator (or user-added CA on userdebug):
    1) Build and install clientVuln or autoVuln
    2) Install your training root CA on the device (user store) — device-specific steps
    3) Run a proxy (Burp/ZAP/mitmproxy) and point device traffic through it (adb reverse/port proxy or Wi‑Fi proxy)
    4) Observe intercepted TLS sessions succeeding because the app trusts the user CA

- Cleartext sniffing:
    1) Use clientVuln/autoVuln
    2) Point DEFAULT_URL to http://<your-lab-host>:<port> 
    3) Capture with Wireshark and show credentials in plaintext

- Pinning failures:
    1) Use clientSecure/autoSecure with real pins configured
    2) Rotate server cert to one not matching the pin
    3) Observe connection failures and app behavior

- CT policy discussion:
    - Discuss how lack of CT (or invalid SCTs) could allow misissued certs to go undetected, and how monitors/logs detect and surface them.


## Where to Insert Your Training Infrastructure

- Replace NetworkHelper.DEFAULT_URL with your server endpoint 
- Add real certificate pins in:
    - src/clientSecure/res/xml/network_security_config_client_secure.xml
    - src/autoSecure/res/xml/network_security_config_auto_secure.xml
    - SecureNetworkHelper CertificatePinner builder (src/secure/...)
- If adding a CT verifier, add the library dependency and wire a validating TrustManager 


## Source Layout

- app/src/main/java/.../MainActivity.kt — Compose UI with a button to run the demo request
- app/src/main/java/.../network/NetworkHelper.kt — interface and DEFAULT_URL
- app/src/secure/java/.../network/SecureNetworkHelper.kt — secure implementation with pinning and CT logging
- app/src/secure/java/.../network/Provider.kt — provideNetworkHelper() for secure flavors
- app/src/vuln/java/.../network/VulnNetworkHelper.kt — intentionally vulnerable implementation
- app/src/vuln/java/.../network/Provider.kt — provideNetworkHelper() for vuln flavors
- Per-flavor manifests and network security configs under app/src/<flavor>/


## AAOS Notes

- AAOS builds share the same code but use distinct network security configs.
- OEMs often control trust stores and policies; system cert store may differ from handheld devices.
- Some head units lack convenient UIs for managing user CAs; secure profiles avoid user CAs entirely.


## Tooling Exercises

- Wireshark: capture cleartext in vuln flavors; inspect TLS handshakes and observe SCT/OCSP stapling
- JADX/apktool: reverse the APK; locate the vulnerable trust manager and hostname verifier in the vuln build
- MobSF: run a static scan and review findings (e.g., cleartextTrafficPermitted)


## Caveats

- This sample is for training only. Do not ship vulnerable configurations.
- Placeholder URLs and pins must be replaced with your lab infrastructure. Look for  markers in code and XML.


## Section 2: Encrypting Data Before Transport (End-to-End Encryption)

This topic demonstrates encrypting sensitive payloads at the application layer before sending them over HTTPS. It covers end-to-end principles, symmetric vs asymmetric options, common pitfalls (encoding vs encryption), and integration with HTTPS. Each security profile (secure vs vuln) shows a contrasting implementation.

How to build and run:
- Select a variant that includes the e2e topic, e.g., clientSecureE2e, clientVulnE2e, autoSecureE2e, autoVulnE2e.
- Launch the app. You’ll see the E2E screen with actions:
    - Encrypt Locally (AES-GCM)
    - Encrypt + Send via HTTPS
    - ECDH Derive Session Key (Demo)
    - Encoding vs Encryption (Base64)

Key code paths:
- UI (topic-specific): app/src/e2e/java/.../E2EActivity.kt
- Crypto API (interface): app/src/main/java/.../crypto/CryptoHelper.kt
- Secure crypto implementation: app/src/secure/java/.../crypto/SecureCryptoHelper.kt
    - AES-GCM with random IV, 128-bit tag
    - HMAC-SHA256 example
    - ECDH key agreement (P-256) to derive a session key (demo only)
    - HTTPS integration: posts encrypted JSON to https://httpbin.org/post (for echo).  set your seminar backend URL that can decrypt
- Vulnerable crypto implementation: app/src/vuln/java/.../crypto/VulnCryptoHelper.kt
    - AES/ECB with static key (pattern leakage, trivial to extract)
    - Misuse of Base64 as “encryption”
    - No integrity protection, permissive hostname verification

Example encrypted JSON shape (secure build):
{
"alg": "AES-GCM",
"iv": base64,
"tag": base64,
"aad": base64,
"ciphertext": base64
}

Notes on key management and exchange:
- Symmetric keys: In this demo, keys are in-memory for simplicity. In real apps, use Android Keystore (Hardware-backed when available) and scoped keys per purpose (encryption vs MAC).
- Key exchange: Secure build shows ECDH (P-256) with a placeholder server public key (PEM). Replace with your server’s real key. 
- Derivation: This sample uses PBKDF2 to derive an AES key from the shared secret to avoid external deps. For production, use HKDF with salt and context info.

Best practices (Android):
- Prefer AES-GCM (or ChaCha20-Poly1305 on Conscrypt/BoringSSL stacks) with random 96-bit IVs.
- Always authenticate data (AEAD like GCM already does), and include AAD when binding to context (e.g., API version or request id).
- Separate keys per purpose; do not reuse encryption keys as MAC keys.
- Rotate keys; include key IDs in payloads; plan for key rollovers.
- Store long-term private keys in Android Keystore; require user auth if appropriate (BiometricPrompt + setUserAuthenticationParameters()).
- Never confuse encoding (Base64) with encryption; Base64 is reversible without a key.
- Validate server certs as usual (this is an app-layer encryption on top of TLS, not a replacement for TLS).

Exploitation exercises (vuln builds):
- Pattern leakage with ECB: show that repeating blocks produce repeating ciphertext. Try modifying the JSON to have repeated patterns and observe the ciphertext changes.
- Static key extraction: Use JADX/apktool to find the hard-coded key in VulnCryptoHelper.
- Base64 as “encryption”: Intercept with a proxy and decode Base64 to recover plaintext instantly.
- Hostname bypass (if routed via vuln networking in your setup): demonstrate MITM feasibility.

AAOS vs client differences:
- AAOS devices may have different TLS stacks or policies; the app-layer encryption is portable but ensure performance constraints (head unit CPUs) are respected. Avoid heavy PBKDF2 iteration counts; prefer HKDF.
- UI interactions may be limited; the training demo keeps actions simple for both form factors.

 checklist for your seminar:
- Replace ECDH server public key with your lab’s key in E2EActivity placeholder.
- Replace HTTPS POST URL in SecureCryptoHelper/VulnCryptoHelper with your API endpoint.
- If you want true server-side decryption, implement a small training backend that accepts the JSON envelope, verifies tag/AAD, and decrypts.
- Optionally integrate Android Keystore-backed keys and HKDF utility.



## Section 3: Reverse Engineering APKs

This topic demonstrates how APKs are decompiled and analyzed and contrasts secure vs vulnerable app decisions. You will explore smali, classes.dex, the manifest, asset leakage, dynamic loading, and signature checks.

How to build and run:
- Select a variant that includes the re topic, e.g., clientSecureRe, clientVulnRe.
- Launch the app. You’ll see the Reverse Engineering screen with actions:
    - Show Hardcoded Secret
    - Read Leaky Asset
    - Attempt Dynamic DEX Load
    - Show App Signature / Verify

Key code paths:
- UI (topic-specific): app/src/re/java/.../REActivity.kt
- Helper API (interface): app/src/main/java/.../re/ReDemoHelper.kt
- Secure implementation: app/src/secure/java/.../re/SecureReDemoHelper.kt
    - No hardcoded secrets, asset not present, dynamic code loading blocked, verifies signing certificate SHA-256 digest ( expected digest).
- Vulnerable implementation: app/src/vuln/java/.../re/VulnReDemoHelper.kt
    - Hardcoded secret in code, ships a sensitive asset, attempts to load external DEX/JAR without verification, no signature verification.
- Asset (vuln only): app/src/vuln/assets/sensitive.txt

Exercises (vuln builds):
1) Find the hardcoded secret:
    - Build clientVulnRe and decode with JADX; locate VulnReDemoHelper.SUPER_SECRET_API_KEY.
2) Asset leakage:
    - Pull the APK or use apktool to decode; retrieve assets/sensitive.txt; identify leaked API key. In-app, tap "Read Leaky Asset" to show content.
3) Dynamic DEX injection:
    - Option A — Self-APK demo (quick): In the app, leave the DEX/JAR field blank or enter 'self', then tap "Attempt Dynamic DEX Load". In vulnerable builds this loads the app’s own APK via DexClassLoader and reports BuildConfig APPLICATION_ID and VERSION_NAME. Secure builds block dynamic loading and display a message including the requested path.
    - Option B — External DEX/JAR: Build a simple JAR containing class dev.training.dynamic.Hello with static String hello(). Convert to DEX: d8 --output out/ dynamic.jar; push to device: adb push out/classes.dex /sdcard/dynamic.jar (or to the app files dir). In the app, enter the path (e.g., /sdcard/dynamic.jar) and tap "Attempt Dynamic DEX Load". Vulnerable builds should load and invoke hello(); secure builds will refuse.
4) App signature:
    - Tap "Show App Signature / Verify" to display the signing certificate SHA-256 digest. In secure builds, set EXPECTED_CERT_DIGEST_B64 in SecureReDemoHelper to enforce verification. 

What obfuscation does/doesn’t do:
- R8/ProGuard can rename symbols and strip unused code, making static analysis harder but not impossible.
- Strings, assets, and native libraries can still leak secrets unless protected; obfuscation is not encryption.

Signature stripping, dynamic loading:
- Discuss how tampered APKs can be resigned; demonstrate why checking the signing cert at runtime helps detect unauthorized repackaging.
- Show risks of DexClassLoader without signature verification and how attackers can inject behavior.

Hardening via R8/ProGuard and native code:
- To try obfuscation, enable minification for release builds (be cautious for classpath of Compose):
    - In app/build.gradle.kts, set isMinifyEnabled = true in the release buildType for the lab.
    - app/proguard-rules.pro already contains baseline rules; add more if necessary (see comments).
- Consider moving critical logic into native code (NDK) or use string encryption — but note determined analysts can still recover it.

AAOS notes:
- Reverse engineering steps are similar on AAOS; ensure head unit allows sideloading lab builds. OEM signing may differ; set the EXPECTED_CERT_DIGEST_B64 accordingly.

 checklist for this section:
- Set SecureReDemoHelper.EXPECTED_CERT_DIGEST_B64 to your signer’s SHA-256 (Base64 NO_WRAP).
- Prepare a dynamic.jar/classes.dex containing dev.training.dynamic.Hello.hello() for the injection demo.
- Replace any placeholder API keys used in demos with lab-specific values if needed.



## Section 4: Permission Internals & App Packaging

This topic provides a low-level view of Android permissions and how packaging/signing and Gradle influence the final APK. It contrasts secure vs vulnerable component configurations using build flavors.

How to build and run:
- Select a variant that includes the perm topic, e.g., clientSecurePerm, clientVulnPerm, autoSecurePerm, autoVulnPerm.
- Launch the app. You’ll see the Permissions & Packaging screen with actions:
    - Show UID/GID & Signing Info
    - Start DemoService
    - Query DemoProvider

Key code paths:
- UI (topic-specific): app/src/perm/java/.../PermActivity.kt
- Helper API (interface): app/src/main/java/.../perm/PermDemoHelper.kt
- Secure implementation: app/src/secure/java/.../perm/SecurePermDemoHelper.kt
- Vulnerable implementation: app/src/vuln/java/.../perm/VulnPermDemoHelper.kt
- Demo components declared in base manifest (secure defaults):
    - Service: dev.jamescullimore.android_security_training.perm.DemoService
    - ContentProvider: dev.jamescullimore.android_security_training.perm.DemoProvider
- Base custom permission (signature-level): dev.jamescullimore.android_security_training.permission.DEMO_SIGNATURE
- Vulnerable manifest overrides: app/src/vuln/AndroidManifest.xml (exports components and removes permission requirements)

What you’ll observe:
- Secure builds: DemoService and DemoProvider are not exported and require a signature-level permission; intra-app access works, but external access fails.
- Vulnerable builds: Components are exported with no protection; external apps can start the service and query the provider (training demonstration only).

Low-level permission model overview:
- Each app runs under a unique Linux UID; runtime permission grants map to per-UID decisions.
- Protection levels:
    - normal: Granted at install; limited risk.
    - dangerous: Requires runtime user consent; scoped to app UID.
    - signature/signatureOrSystem: Granted only if the caller is signed with the same cert (or on privileged/system builds for legacy system perms).
- System UIDs/GIDs: Preinstalled/privileged components can hold system-only permissions; third-party apps cannot. AAOS OEM images may include additional privileged permissions.

Manifest merging:
- Base manifest defines secure defaults (non-exported components requiring a signature-level permission).
- Vulnerable flavor manifest (src/vuln/AndroidManifest.xml) uses tools:replace to change attributes (exported=true, remove permissions, grantUriPermissions=true).
- Use Android Studio’s Merged Manifest view to see the final result per variant.

APK contents and Gradle influences:
- APK structure: AndroidManifest.xml, classes.dex, res/ resources, assets/, lib/, META-INF/ (V1/V2/V3 signatures), and (for V4) a separate .idsig file.
- Signing schemes:
    - V1 (JAR): Legacy; resigning breaks digest but remains installable on very old devices.
    - V2/V3: Sign whole APK; detects repackaging. Required on modern Android.
    - V4: For incremental installs via ADB/Play; companions .idsig file. Useful for large apps; integrity checked by package manager.
- Gradle affects outputs via:
    - buildTypes (debug/release), minifyEnabled/R8, shrinkResources.
    - productFlavors (formFactor/securityProfile/topic) which also influence manifest merging and resource packaging.

Build process and hardening tips:
- Prefer non-exported components; if exported, enforce custom permissions (signature if only your apps should call).
- Validate callers with checkCallingPermission() or enforce at manifest level; avoid runtime "manual" checks when manifest-level can enforce.
- Avoid world-readable files; use MODE_PRIVATE and Storage Access Framework.
- Use R8/ProGuard judiciously; obfuscation is not a security control but can raise the bar.
- Consider runtime self-signature checks to detect repackaging (see Reverse Engineering section), Play Integrity API for device/app integrity signals.

Exercises:
1) Component exploitation (vuln builds):
    - Build clientVulnPerm.
    - From adb shell or a helper app, start the service:
      adb shell am startservice -n dev.jamescullimore.android_security_training.client.vuln.perm/dev.jamescullimore.android_security_training.perm.DemoService
    - Query the provider:
      adb shell content query --uri content://dev.jamescullimore.android_security_training.client.vuln.perm.demo/hello
    - Observe success due to exported components.
2) Secure build checks:
    - Repeat with clientSecurePerm. Calls should fail with SecurityException or permission denial.
3) Merged manifest:
    - Open Merged Manifest for both variants; identify where tools:replace changed attributes.
4) Packaging and signing:
    - Build a release APK and verify signatures:
      ./gradlew :app:assembleRelease
      apksigner verify --print-certs app/build/outputs/apk/clientSecurePerm/release/app-client-secure-perm-release.apk
    - Note the SHA-256 cert digest; compare with the in-app display.

AAOS notes:
- Privileged/system permissions may be present on AAOS/OEM builds; third-party apps cannot obtain them. Demonstrations still hold for exported components and custom signature permissions.

 checklist for your seminar:
- If you want to demonstrate cross-app signature permissions, build a tiny second app signed with the same key to successfully call into the secure component.
- Add a signingConfig for release with your keystore in app/build.gradle.kts for realistic signature digests (do not commit secrets):
  signingConfigs {
  create("seminar") {
  storeFile = file("release.jks")
  storePassword = "Test123"
  keyAlias = "key0"
  keyPassword = "Test123"
  }
  }
  buildTypes { release { signingConfig = signingConfigs.getByName("seminar") } }
- Optionally add a broadcast receiver demo with exported=true in vuln and intent-filter to show unauthorized broadcasts.



## Exercise: Reverse Engineering APKs and Hardening

This exercise is fully doable with the current repository. The project already contains a dedicated Reverse Engineering (re) topic with both secure and vulnerable implementations, intentionally exposed behaviors, a leaky asset (vuln only), and ProGuard/R8 configuration. Below is a turnkey lab plan you can run with your class.

What you will accomplish:
- Use jadx and apktool (optionally MobSF) to inspect the APK.
- Locate common misconfigurations: cleartext allowance, exported components, hardcoded secrets, leaky assets, and dynamic loading.
- Patch the APK to bypass a simple check (e.g., signature verification), then re‑sign and reinstall to observe impact.
- Enable ProGuard/R8 and compare analysis results pre/post-obfuscation.

Preconditions
- Android Studio (latest), adb in PATH
- jadx and apktool installed; optionally MobSF
- Emulator or device (USB debugging enabled)
- This repository (provides source and APK outputs)

Which build variants to use
- Vulnerable for analysis and patching: clientVulnRe
- Secure for comparison: clientSecureRe

Prepare APKs
- Build debug APKs (example vulnerable):
    - ./gradlew :app:assembleClientVulnReDebug
    - Outputs under app/build/outputs/apk/clientVulnRe/debug/
- Optionally also build secure for comparison:
    - ./gradlew :app:assembleClientSecureReDebug

Part 1 — Reverse with jadx/apktool (30–40 min)
1) Open APK in jadx-gui:
    - Identify hardcoded secret in vuln build: dev.jamescullimore.android_security_training.re.VulnReDemoHelper (field SUPER_SECRET_API_KEY).
    - Find dynamic loading logic: tryDynamicDexLoad using DexClassLoader.
    - Compare secure vs vuln: SecureReDemoHelper.verifyExpectedSignature() enforces signing cert digest; VulnReDemoHelper returns true always.
2) Decode APK with apktool:
    - apktool d app-client-vuln-re-debug.apk -o out_vuln
    - Inspect AndroidManifest.xml for exported components.
        - REActivity is exported (launcher).
        - For component abuse demos, also see the perm topic where DemoService and DemoProvider are exported in vuln builds (src/vuln/AndroidManifest.xml) — you can cross‑reference here.
    - Check res/xml/network_security_config_* in clientVuln flavors (cleartextTrafficPermitted=true, trusts user CAs).
    - Inspect assets/ directory: sensitive.txt exists only in vuln builds and contains demo secrets.
3) Optional MobSF:
    - Run a quick scan to surface cleartextTrafficPermitted and exported components.

Part 2 — Patch and re‑sign (20–30 min)
Goal: Bypass a simple runtime check in the secure build, then re‑sign and install.
A) Choose a target:
- SecureReDemoHelper.verifyExpectedSignature(Context): returns false unless the signing cert digest matches EXPECTED_CERT_DIGEST_B64.
- You will flip this to always return true.
  B) Steps with apktool/baksmali:
- Build secure APK: ./gradlew :app:assembleClientSecureReRelease (or Debug).
- Decode APK: apktool d app-client-secure-re-release.apk -o out_secure
- Find smali for SecureReDemoHelper (path similar to out_secure/smali_classesN/dev/jamescullimore/android_security_training/re/SecureReDemoHelper.smali).
- Locate method verifyExpectedSignature; modify its body to return const/4 v0, 0x1 and replace any conditional branches accordingly, ensuring a final return v0 (true).
- Rebuild APK: apktool b out_secure -o secure_patched.apk
  C) Re‑sign the APK:
- Using the Android debug keystore (for lab convenience):
    - Key file: ~/.android/debug.keystore
    - Alias: androiddebugkey
    - Keystore password: android
    - Key password: android
- apksigner sign --ks ~/.android/debug.keystore --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android \
  --out secure_patched_signed.apk secure_patched.apk
- Verify signature: apksigner verify --print-certs secure_patched_signed.apk
  D) Install and test:
- adb install -r secure_patched_signed.apk
- Launch clientSecureRe (patched) and tap "Show App Signature / Verify" — Verified should now show true due to your patch.

Notes on signature schemes and integrity
- Modern Android enforces V2/V3 signing; apksigner handles this. Repackaging will change the signing cert; the secure build checks its signer digest to detect tampering. Your patch demonstrates how an attacker can disable such checks if they can modify and re‑sign the APK.

Part 3 — Observe exposed components and deep links (10–15 min)
- Activities:
    - REActivity is exported to be the launcher. You can craft an explicit intent to start it:
      adb shell am start -n dev.jamescullimore.android_security_training.client.vuln.re/dev.jamescullimore.android_security_training.REActivity
- Services/Providers (from Permission Internals topic):
    - In clientVulnPerm builds, DemoService and DemoProvider are exported without protection; demonstrate unauthorized access:
        - Start service:
          adb shell am startservice -n dev.jamescullimore.android_security_training.client.vuln.perm/dev.jamescullimore.android_security_training.perm.DemoService
        - Query provider:
          adb shell content query --uri content://dev.jamescullimore.android_security_training.client.vuln.perm.demo/hello
- Cleartext and user CAs:
    - In clientVulnPinning/clientVulnRe, Network Security Config permits cleartext and trusts user CAs. Use Wireshark/mitmproxy to observe traffic.

Part 4 — Apply ProGuard/R8 and compare (20–30 min)
1) Enable minification for release only (recommended for lab):
    - In app/build.gradle.kts, set in buildTypes.release: isMinifyEnabled = true
    - Keep proguardFiles as configured (proguard-android-optimize.txt, proguard-rules.pro).
2) Build a release APK (example vulnerable):
    - ./gradlew :app:assembleClientVulnReRelease
3) Inspect with jadx/apktool again:
    - Observe class and method name obfuscation; strings remain visible unless you add string encryption plugins.
    - mapping.txt is generated under app/build/outputs/mapping/<variant>/mapping.txt (useful to map obfuscated names back during debugging).
4) Discussion:
    - What obfuscation helps with (raising reverse‑engineering effort) vs. what it doesn’t (assets/secrets not encrypted, dynamic behavior still present).

Troubleshooting
- INSTALL_PARSE_FAILED_NO_CERTIFICATES: Ensure you signed the rebuilt APK with apksigner.
- INSTALL_FAILED_UPDATE_INCOMPATIBLE: Use the same applicationId or uninstall previous variant: adb uninstall <app_id>.
- If apktool rebuild fails, try using a release build (less debug resources) or ensure you didn’t break smali syntax.

Homework
- Read OWASP MASVS chapters on code obfuscation and binary protections.
- Watch “Android Application Security Testing with MobSF”.
- Prepare a short report listing at least 3 risks you found (e.g., hardcoded secrets, exported components, cleartext, dynamic code loading) and propose mitigations for each.

### Section 5: Deep Links

This topic demonstrates security pitfalls of deep links and app links: BROWSABLE exposure, auto-verification, hijackable links, and data leakage. Secure vs vulnerable implementations differ via build flavors, with client and AAOS supported.

How to build and run:
- Select a variant that includes the links topic, e.g., clientSecureLinks, clientVulnLinks, autoSecureLinks, autoVulnLinks.
- Launch the app. You’ll see the Deep Links screen that shows the received Intent and allows crafting VIEW intents.

Key code paths:
- UI (topic-specific): app/src/links/java/.../DeepLinksActivity.kt
- Helper API (interface): app/src/main/java/.../deeplink/DeepLinkHelper.kt
- Secure implementation: app/src/secure/java/.../deeplink/SecureDeepLinkHelper.kt
- Vulnerable implementation: app/src/vuln/java/.../deeplink/VulnDeepLinkHelper.kt
- Manifests:
    - Topic: app/src/links/AndroidManifest.xml (sets DeepLinksActivity as launcher)
    - Secure flavor: app/src/secure/AndroidManifest.xml (https only, autoVerify=true)  host
    - Vulnerable flavor: app/src/vuln/AndroidManifest.xml (broad http/https pathPrefix=/)

What you’ll observe:
- Secure builds: only https://<host>/welcome or /auth/callback paths are accepted; state parameter is required; code is redacted.
- Vulnerable builds: http and https are accepted broadly; parameters are echoed and logged; unsafe external navigation is possible.

Exercises:
1) App links verification
    - Set your domain () in secure manifest and SecureDeepLinkHelper.
    - Host .well-known/assetlinks.json on your domain mapping to your app’s package and signing cert digest.
    - Install clientSecureLinks and run: adb shell am start -a android.intent.action.VIEW -d "https://<host>/welcome?code=123&state=abc" <appId>
    - Observe that the app opens directly when verification succeeds.
2) Hijackable links (vuln)
    - Build clientVulnLinks.
    - Send an http:// link: adb shell am start -a VIEW -d "http://lab.example.com/welcome?code=leak&state=nope" <appId>
    - Notice acceptance and logging of raw parameters; discuss network tampering and phishing vectors.
3) Data validation
    - In secure build, omit state or change host; verify rejection.
    - In vuln build, try file:// or intent:// URIs (some may fail to parse), and note lack of validation.
4) Best practices (auth and navigation)
    - Use HTTPS app links with autoVerify and host-only filters.
    - Require state/nonce (PKCE for OAuth); never log secrets; do not trust extras.
    - Validate scheme/host/path; consider setPackage on internal intents.

AAOS notes:
- App links behave similarly; ensure the head unit browser and resolver support app links. OEM images may alter default handlers; verification improves reliability.

 checklist for your seminar:
- Replace lab.example.com in SecureDeepLinkHelper and secure manifest with your real domain.
- Publish .well-known/assetlinks.json containing your package name and signing certificate SHA-256 digest (Base64). Guide: https://developer.android.com/training/app-links/verify-site-associations
- Optionally add a second app signed with a different key that claims the same host to demonstrate chooser/hijacking when autoVerify is absent.

# Android Security Training — Sections: (1) Certificate Pinning & Transparency, (2) Encrypting Data Before Transport

This repository contains an example Android app set up for security training exercises. This section focuses on the HTTPS/TLS stack, certificate pinning, dangers of user-installed CAs, handling updates/pin rollouts, and especially Certificate Transparency (CT). Each topic includes both a secure implementation and an intentionally vulnerable one, separated by build variants. Client and Automotive (AAOS) form factors are both represented.

Note: Some values are placeholders with  so you can point the app at your own training infrastructure later.


## Prerequisites and Environment Requirements

- Experience developing on embedded Linux
- Basic understanding of app-level security (TLS, file system, permissions)
- No prior Android experience needed
- Android Studio (latest)
- adb (in PATH)
- Wireshark
- JADX, apktool, MobSF
- Physical or emulator device with Google Play services (rooted and non‑rooted)
- Non-congested Wi‑Fi network with internet access


## Project Overview

This app now uses three flavor dimensions to separate concerns:

- Form factor: client
- Security profile: secure, vuln
- Training topic: pinning, e2e, re, perm, links, storage

Examples of combined variants (Build Variants in Android Studio):
- clientSecurePinning — Secure client example for Certificate Pinning & CT
- clientVulnPinning — Vulnerable client example for pinning topic
- clientSecureE2e — Secure client example for Encrypting Data Before Transport
- clientVulnE2e — Vulnerable client example for E2E encryption topic
- clientSecureRe — Secure client example for Reverse Engineering topic
- clientVulnRe — Vulnerable client example for Reverse Engineering topic
- clientSecurePerm — Secure client example for Permission Internals topic
- clientVulnPerm — Vulnerable client example for Permission Internals topic
- clientSecureLinks — Secure client example for Deep Links
- clientVulnLinks — Vulnerable client example for Deep Links
- clientSecureStorage — Secure client example for Local Data Storage
- clientVulnStorage — Vulnerable client example for Local Data Storage

Key implementation points:
- Network Security Config per variant under src/<flavor>/res/xml/
- OkHttp used for HTTP(S) with logging
- Secure variants use standard TrustManager + hostname verification + optional pinning
- Vulnerable variants use trust-all TrustManager + hostname bypass (for training ONLY)
- Topic-specific Activities per flavor under src/<topic>/ manifest/source sets

[The rest of the document below remains unchanged up to the end]

## Section 6: Local Data Storage — App Data, Tokens, Preferences

This topic demonstrates secure and insecure patterns for storing sensitive data locally. It covers SharedPreferences, internal vs external storage, EncryptedSharedPreferences, EncryptedFile, and the importance of Context.MODE_PRIVATE and file locations.

How to build and run:
- Select a variant that includes the storage topic, e.g., clientSecureStorage, clientVulnStorage, autoSecureStorage, autoVulnStorage.
- Launch the app. You’ll see the Local Data Storage screen with actions to save/load tokens and write/read files in secure and insecure ways.

Key code paths:
- UI (topic-specific): app/src/storage/java/.../StorageActivity.kt
- Helper API (interface): app/src/main/java/.../storage/StorageHelper.kt
- Secure implementation: app/src/secure/java/.../storage/SecureStorageHelper.kt
    - EncryptedSharedPreferences for token storage
    - EncryptedFile for file storage in internal app directory
    - Comparison method that writes to plaintext cache for discussion
- Vulnerable implementation: app/src/vuln/java/.../storage/VulnStorageHelper.kt
    - Plain SharedPreferences for tokens
    - Plaintext files in app-external and public Downloads directories to illustrate leakage risk

What you’ll observe:
- Secure builds:
    - Tokens are stored encrypted at rest via EncryptedSharedPreferences.
    - Files written with EncryptedFile under filesDir are unreadable without app/master key.
- Vulnerable builds:
    - Tokens stored in plaintext SharedPreferences.
    - Files written as plaintext to external/public locations, potentially accessible by other apps or via USB/user grants.

Buttons in StorageActivity:
- Save Token (EncryptedSharedPreferences)
- Load Token (Encrypted)
- Save Token (Plain SharedPreferences)
- Load Token (Plain)
- Write Secure File (EncryptedFile)
- Write Insecure File (Plaintext)
- Read Insecure File

Best practices:
- Prefer Context.MODE_PRIVATE and internal app storage (filesDir, no-backup dir) for sensitive data.
- Use EncryptedSharedPreferences for tokens/secrets instead of plain prefs.
- Use EncryptedFile (or a database with field/file encryption) for sensitive files.
- Avoid writing secrets to external storage (even app-specific) or public directories.
- Clear sensitive caches and consider on-device compromise scenarios (root/jailbreak). Combine with device policy, biometrics, and hardware-backed keys where available.

AAOS notes:
- On shared head units, external/public storage may be visible to multiple users or OEM apps. Prefer internal storage and encryption. Performance constraints may favor lightweight operations.

Exercises:
1) Compare plaintext vs encrypted prefs
    - Build clientVulnStorage; tap Save/Load Token (Plain). Pull /data/data/<pkg>/shared_prefs/insecure_prefs.xml via rooted device/emulator and show plaintext. Repeat on secure build and observe ciphertext in EncryptedSharedPreferences file.
2) File leakage
    - In vuln build, tap Write Insecure File; then pull it from /sdcard/Downloads/insecure.txt or via the device’s Files app.
    - In secure build, tap Write Secure File; attempt to read the file outside the app (it’s encrypted and in internal storage).
3) MODE_PRIVATE and locations
    - Discuss why MODE_WORLD_READABLE is long-gone and why external storage is still risky. Show cacheDir vs filesDir.
4) Bonus: FileProvider
    - Add a FileProvider to share a file intentionally and demonstrate grantUriPermissions and scoped access (optional extension).

 checklist for your seminar:
- If you want to demo Keystore-backed file keys separately, add Android Keystore integration and show key invalidation on biometrics/pin removal.
- If using real tokens, rotate and invalidate after lab; do not ship demo secrets.

References:
- EncryptedSharedPreferences and EncryptedFile: https://developer.android.com/topic/security/data
- Storage best practices: https://developer.android.com/training/data-storage
- App data and files: https://developer.android.com/training/data-storage/app-specific



## Section 7: Anti-Root Protections

This topic demonstrates detecting common root/jailbreak indicators, what an app might block, how attackers often bypass such detections, and why detections are inherently limited. It also covers a high-level overview of SafetyNet/Play Integrity, and basics of obfuscation and tamper-resistance.

How to build and run:
- Select a variant that includes the root topic, e.g., clientSecureRoot, clientVulnRoot, autoSecureRoot, autoVulnRoot.
- Launch the app. You’ll see the Anti-Root screen with actions to run checks, simulate blocking, toggle a bypass (vuln only), and view a placeholder Play Integrity status.

Key code paths:
- UI (topic-specific): app/src/root/java/.../RootActivity.kt
- Helper API (interface): app/src/main/java/.../root/RootHelper.kt
- Secure implementation: app/src/secure/java/.../root/SecureRootHelper.kt
    - Non-privileged checks (heuristic):
        - su binary presence (/system/bin/su, /system/xbin/su, /sbin/su, etc.)
        - BusyBox presence
        - Magisk/Zygisk traces (/sbin/.magisk, /data/adb/magisk, modules)
        - Build tags test-keys (Build.TAGS / FINGERPRINT)
        - SELinux enforcing status (best-effort)
        - /system mount read-write heuristic (from /proc/mounts)
    - Simple self-tamper check comparing signing cert SHA-256 digest ( set expected digest)
- Vulnerable implementation: app/src/vuln/java/.../root/VulnRootHelper.kt (always returns not rooted; has a bypass toggle)
- Providers: provideRootHelper() added to secure/vuln Provider.kt.

What you’ll observe:
- Secure builds: Multiple signals are gathered and displayed; if a strong signal is present (su/magisk/system rw), isRooted() returns true.
- Vulnerable builds: isRooted() effectively always returns false; you can toggle a “bypass” to simulate how fragile naive detections can be.

SafetyNet / Play Integrity API overview:
- SafetyNet Attestation is deprecated; use the Play Integrity API for device/app integrity verdicts from Google Play services. This sample shows a placeholder string.
- Integration requires a Play Console project, server-side verification of the signed JWS, and exchanging a nonce. In classroom settings, wire this only if you can provision credentials.  add your integration if desired.
- Signals include deviceIntegrity (MEETS_STRONG/MEETS_BASIC), appIntegrity, and account checks depending on your configuration. Use verdicts to gate sensitive actions; never rely solely on client-side checks.

Obfuscation and tamper-resistance basics:
- Use R8/ProGuard to obfuscate/strip code. This raises effort but is not a security boundary.
- Consider runtime signature checks (as shown) to detect repackaging. Attackers can patch these checks; treat this as telemetry, not a guarantee.
- Native code, string encryption, and anti-debug/hook heuristics can add friction but are bypassable.

Limitations of detection:
- Root detection is heuristic and can produce false positives/negatives. Magisk/Zygisk DenyList, LSPosed, and Frida hooks can hide artifacts or tamper with checks.
- Treat detections as risk signals feeding into server policy decisions (e.g., additional verification, reduced feature set), not as absolute blockers.

Exercises:
1) Compare secure vs vulnerable builds
    - Run clientSecureRoot and clientVulnRoot. Tap “Run Root Checks.” Compare signal lists and isRooted().
2) Simulate root with an emulator image or a rooted test device
    - On a rooted emulator/device, create a dummy su binary path and rerun checks; observe the change.
3) Tamper check
    - In SecureRootHelper, set EXPECTED_SIGNER_DIGEST_B64 to your signer’s SHA-256 (Base64). Rebuild and verify the tamper check.
4) Discuss bypass techniques
    - Explain Magisk/Zygisk DenyList, Frida/LSPosed hook approaches that patch isRooted() to always return false (mirrors our vuln toggle).
5) (Optional) Wire Play Integrity
    - Add client and server as per Google’s guide; display verdicts in the UI and enforce a simple policy.  placeholders in code.

AAOS notes:
- Rooted head units are less common in production. OEM/debug builds may carry test-keys and additional system apps. Detections can behave differently; treat signals with caution and prefer server-side policies when possible.

 checklist for your seminar:
- Set SecureRootHelper.EXPECTED_SIGNER_DIGEST_B64 to your signer digest (Base64 NO_WRAP).
- If integrating Play Integrity, add configuration and server components; display real verdicts.
- Prepare a rooted emulator/device or Magisk demo for the lab.



## Exercise: Local Data Manipulation and Anti-Root Interaction (Storage + Root)

This exercise is fully doable with the current repository. The project now includes:
- A Local Data Storage topic (storage) demonstrating EncryptedSharedPreferences, EncryptedFile, and plaintext paths.
- A small SQLite database demo (tokens.db) for practicing adb/sqlite3 reads and modifications.
- Anti-root detection (root topic) and, for demonstration, StorageActivity consults the RootHelper to block certain sensitive actions in secure builds when root is detected.

What participants will do
- Explore app data storage locations on a non-rooted device via adb shell and Android Studio Device Explorer.
- Attempt to access and modify SharedPreferences, SQLite DB, and files.
- Switch to a rooted emulator/device to bypass restrictions and modify app data (e.g., tokens, flags).
- Test anti-root detection logic and observe how it blocks/permits actions.
- Use adb, sqlite3, and optionally Frida for dynamic inspection.
- Validate how EncryptedSharedPreferences and EncryptedFile protect data and discuss failure cases if key management is weak.

Which build variants to use
- Vulnerable: clientVulnStorage (or autoVulnStorage) — easy to inspect (plaintext prefs/files/DB, no real root gating effect).
- Secure: clientSecureStorage (or autoSecureStorage) — demonstrates encrypted prefs/files and root-gated actions.

Key code paths for this lab
- UI: app/src/storage/java/.../StorageActivity.kt
    - Adds DB operations (Put/Get/List/Delete) and calls RootHelper to block some actions when rooted.
- Helper API: app/src/main/java/.../storage/StorageHelper.kt
    - Now includes dbPut/dbGet/dbList/dbDelete.
- Secure implementation: app/src/secure/java/.../storage/SecureStorageHelper.kt
    - EncryptedSharedPreferences + EncryptedFile; plaintext SQLite demo (training only) at /data/data/<pkg>/databases/tokens.db
- Vulnerable implementation: app/src/vuln/java/.../storage/VulnStorageHelper.kt
    - Plaintext SharedPreferences and files (public Downloads), plaintext SQLite DB.
- Root helpers: app/src/.../root/* and provider wiring in secure/vuln Provider.kt

Step-by-step (60–90 min)
1) Non-rooted exploration (15–20 min)
    - Build and install clientVulnStorage.
    - Device Explorer (Android Studio): Inspect
        - shared_prefs/insecure_prefs.xml — tokens in plaintext.
        - databases/tokens.db — exists; you can download but not modify in place without root.
        - files/ and cache/ — check plaintext file created by "Write Insecure File" button (vuln writes to Downloads; pull from /sdcard/Downloads/insecure.txt).
    - Try clientSecureStorage: Observe that secure_prefs file content is encrypted (random bytes); secure.txt lives under filesDir and is encrypted (can’t read outside the app).

2) SQLite with adb and sqlite3 (15–20 min)
    - In app (either variant), press DB Put/Get/List/Delete buttons to create rows.
    - On a rooted device/emulator:
        - su to root and inspect the DB directly:
          adb shell
          su
          cd /data/data/<app_id>/databases
          sqlite3 tokens.db "SELECT k, v FROM tokens;"
        - Modify data:
          sqlite3 tokens.db "UPDATE tokens SET v='pwned' WHERE k='session_token';"
        - In app, press "DB Get" to observe the change.
    - On a non-rooted device:
        - Use Device Explorer to pull tokens.db, modify on host with sqlite3, and push back is typically blocked; discuss sandboxing and why root is needed for direct modification.

3) Files and preferences (10–15 min)
    - Vulnerable build:
        - Press Write Insecure File; view /sdcard/Downloads/insecure.txt (accessible via Files app/USB).
        - Press Save Token (Plain); pull shared_prefs/insecure_prefs.xml via rooted device to show plaintext.
    - Secure build:
        - Press Save Token (Encrypted) and Write Secure File; confirm encryption at rest (content not human-readable and file is internal).

4) Anti-root gating (10–15 min)
    - Build clientSecureStorage and run on rooted emulator/device.
    - Tap buttons that perform secure writes (EncryptedSharedPreferences, EncryptedFile, DB Put). These are guarded and will display a message if root is detected.
    - Compare with clientVulnStorage where the weak root helper effectively allows actions (demonstrates bypassable client checks).
    - For deeper exploration, switch to the root topic (clientSecureRoot) to view detailed signals.

5) Optional advanced: Frida hooks and dynamic inspection (10–20 min)
    - Example ideas (not included in code):
        - Hook SharedPreferences.getString to read sensitive keys.
        - Hook SQLiteDatabase.execSQL to log queries/parameters.
        - Patch RootHelper.isRooted() to always return false and observe the guarded actions proceed.

Commands quick reference
- List device files (non-root):
  adb shell run-as <app_id> ls -l /data/data/<app_id>/files  # may work on debuggable builds
- Copy public file:
  adb pull /sdcard/Downloads/insecure.txt
- SQLite (rooted):
  adb shell su -c "sqlite3 /data/data/<app_id>/databases/tokens.db 'SELECT k, v FROM tokens;'"
- View prefs (rooted vuln build):
  adb shell su -c "cat /data/data/<app_id>/shared_prefs/insecure_prefs.xml"

What to discuss
- Why EncryptedSharedPreferences/EncryptedFile protect data at rest but rely on key management; discuss MasterKey storage and biometrics/keystore options.
- SQLite encryption requires additional tooling (e.g., SQLCipher for Android). : If you want encrypted DB in class, add SQLCipher dependency and switch the demo to it.
- Root detection is heuristic and bypassable; use detections as signals fed into server policy.

Homework
- Read: OWASP MASVS — Storage of Sensitive Data.
- Optional: Watch “Introduction to Frida for Mobile Reverse Engineering”.
- Write a short summary: three key findings about data manipulation and how to mitigate them in your apps.


## Section 8: WebView and Component Security

This topic highlights WebView attack surface and Android component pitfalls (broadcast abuse, PendingIntent leaks, and provider exposure). Secure vs vulnerable variants are implemented under the new topic "web".

How to build and run:
- Select a variant that includes the web topic, e.g., clientSecureWeb, clientVulnWeb, autoSecureWeb, autoVulnWeb.
- Launch the app. You’ll see the WebView & Component Security screen with controls to configure the WebView, load URLs, run JS, and trigger component demos.

Key code paths:
- UI (topic): app/src/web/java/.../WebActivity.kt
- Helper API: app/src/main/java/.../web/WebViewHelper.kt
- Secure implementation: app/src/secure/java/.../web/SecureWebViewHelper.kt
    - Safe defaults: Safe Browsing enabled, mixed content blocked, file:// access disabled, strict host allowlist, no addJavascriptInterface secrets.
- Vulnerable implementation: app/src/vuln/java/.../web/VulnWebViewHelper.kt
    - JS bridge exposing token via addJavascriptInterface, mixed content allowed, file:// access enabled, no URL validation.
- Broadcast receiver (demo): app/src/main/java/.../web/DemoReceiver.kt
    - Secure: not exported (base manifest).
    - Vulnerable: exported=true with intent-filters (app/src/vuln/AndroidManifest.xml).

Buttons in WebActivity:
- Configure WebView: Applies secure/vulnerable settings depending on build.
- Load Trusted URL (https): Both builds load https://lethalmaus.github.io/AndroidSecurityTraining/README.md (primary trusted page).
- Load Untrusted URL (http): Secure attempts to load http://neverssl.com but blocks via allowlist; vuln loads it to demonstrate cleartext and lack of validation.
- Run JS Demo / Bridge Call: In vuln builds, JS calls Android.leakToken() and broadcasts it; secure builds execute a harmless alert only. You’ll see Logcat tag "WebDemo" and on-device Toasts confirming receipt.
- Send Internal Broadcast: Both builds send a package-scoped broadcast (setPackage); receiver is non-exported in secure builds and exported in vuln builds (for training), but targeting ensures the demo works reliably.
- Expose PendingIntent (demo): Secure creates an immutable PI and does not expose it; vuln leaks a mutable Activity PendingIntent via broadcast action dev…LEAK_PI targeted to this app. DemoReceiver shows Toasts and triggers the PI, which may bring WebActivity to the foreground.

Exercises (30–45 min):
1) JS bridge exfiltration (vuln)
    - Build clientVulnWeb.
    - Tap Configure WebView, then Run JS Demo; observe Logcat tag "WebDemo" from DemoReceiver with exfil:API_TOKEN_ABC123 and a DEMO Toast.
    - Optional: host your own page to call Android.leakToken(); by default the built-in demo uses evaluateJavascript to exercise the bridge.
2) Mixed content risks (vuln)
    - Tap Load Trusted URL and Load Untrusted URL; show that http://neverssl.com loads in vuln builds and is blocked in secure builds.
3) Broadcast abuse
    - On vuln build, trigger the exported receiver from adb:
      adb shell am broadcast -a dev.jamescullimore.android_security_training.web.DEMO --es msg "pwned" --receiver-foreground
    - Compare with secure build: receiver is not exported and broadcasts are package-scoped (setPackage).
4) PendingIntent hijack (vuln)
    - Tap Expose PendingIntent (demo). You will see LEAK_PI toasts and the receiver will attempt to trigger the leaked mutable PI, potentially bringing WebActivity to the foreground. Discuss why FLAG_IMMUTABLE is critical on Android 12+.

Best practices:
- WebView
    - Avoid addJavascriptInterface unless absolutely required; never expose secrets, prefer postMessage channels, and validate inputs strictly.
    - Enforce https and a strict allowlist in shouldOverrideUrlLoading.
    - Disable file access and universal access from file URLs; block mixed content.
    - Enable Safe Browsing (API 26+).
- Components
    - Prefer non-exported receivers; if exported, require permissions and validate caller identity.
    - Use FLAG_IMMUTABLE for PendingIntent; don’t share sensitive PIs.
    - Keep providers non-exported or protected by permissions; validate and scope URIs.

AAOS notes:
- WebView availability and Safe Browsing can vary with OEM images; verify features on target head unit.
- Broadcast/component behavior is similar; ensure exported settings are deliberate.

 checklist for your seminar:
- Allowed hosts (default): lethalmaus.github.io (primary), jamescullimore.dev (legacy). Update SecureWebViewHelper if you host your own trusted page.
- Untrusted demo: http://neverssl.com (vuln loads; secure blocks). Optionally host a malicious page to call Android.leakToken() if you want remote JS control.
- PendingIntent demo requires no helper app: tap "Expose PendingIntent (demo)" in vuln build and observe LEAK_PI toasts; the receiver will trigger the leaked PI automatically.

References:
- WebView security best practices: https://developer.android.com/guide/webapps/managing-webview
- addJavascriptInterface risks: https://labs.mwrinfosecurity.com/blog/webview-addjavascriptinterface-remote-code-execution/
- PendingIntent mutability: https://developer.android.com/reference/android/app/PendingIntent#FLAG_IMMUTABLE



## Exercise: WebView and Component Security — Exploit and Harden

This exercise is fully doable with the current repository. The Web topic already provides both intentionally insecure (vuln) and hardened (secure) implementations, plus an exported component in vuln builds for abuse demonstrations.

What you will do
- Analyze a demo app with an intentionally insecure WebView (addJavascriptInterface, unvalidated URLs) in vuln builds.
- Load malicious URLs and run JavaScript to observe injection and data exfiltration.
- Abuse exported components by sending broadcasts from adb and observe effects.
- Apply mitigations (manifest and code), then retest to verify fixes.

Which build variants to use
- Vulnerable: clientVulnWeb (or autoVulnWeb)
- Secure baseline: clientSecureWeb (or autoSecureWeb)

Prerequisites
- Android Studio (latest), adb in PATH
- Emulator or device
- Optional: a simple HTTP server you control to serve test pages ( set hosts in helpers/manifests)

Key code and manifests for this lab
- UI: app/src/web/java/.../WebActivity.kt (contains buttons to configure and load WebView, run JS demo, and trigger component actions)
- Helpers: app/src/main/java/.../web/WebViewHelper.kt (API); per-security implementations:
    - Secure: app/src/secure/java/.../web/SecureWebViewHelper.kt (hardened)
    - Vulnerable: app/src/vuln/java/.../web/VulnWebViewHelper.kt (intentionally unsafe)
- Broadcast receiver: app/src/main/java/.../web/DemoReceiver.kt; declared not exported in base manifest (secure), overridden to exported=true with extra intent actions in app/src/vuln/AndroidManifest.xml
- Topic manifest: app/src/web/AndroidManifest.xml (WebActivity is the launcher for this topic)

Step-by-step (45–75 min)
1) Recon and run (5–10 min)
    - Build and install clientVulnWeb.
    - Open the app; tap “Configure WebView” (applies insecure settings in vuln build).
    - Tap “Load Untrusted URL” and “Run JS Demo”. Watch Logcat for tag "WebDemo".

2) JS bridge exfiltration (10–15 min)
    - In vuln build, a JS bridge is exposed via addJavascriptInterface with object name Android.
    - From an attacker-controlled page ( host), include a script that runs:
        - Android.leakToken && Android.leakToken();
    - Alternatively, use the in-app demo: after Configure WebView, tap Run JS Demo to trigger bridge use and observe logcat exfiltration.
    - Logcat command example:
        - adb logcat | grep -i WebDemo

3) Unvalidated URL loading (10–15 min)
    - Send explicit intents to the activity to load http/file URLs (vuln build accepts broadly). Example:
        - adb shell am start -n "<app_id>/.WebActivity" \
          --es url "http://neverssl.com/"   # demo HTTP page for cleartext
    - Try file URLs (prepare a file first):
        - adb shell 'echo hello > /sdcard/Download/insecure.txt'
        - adb shell am start -n "<app_id>/.WebActivity" --es url "file:///sdcard/Download/insecure.txt"
    - Observe that vuln build loads; secure build should block by scheme/host checks and shouldOverrideUrlLoading.

4) Broadcast abuse (10–15 min)
    - In vuln build, DemoReceiver is exported with action dev.jamescullimore.android_security_training.web.DEMO.
    - Trigger it from adb:
        - adb shell am broadcast -a dev.jamescullimore.android_security_training.web.DEMO --es msg "pwned"
    - Expected: WebActivity/DemoReceiver log shows the broadcast content in vuln builds; secure builds ignore (receiver not exported and/or package-scoped sends only).

5) PendingIntent leak (optional, 10 min)
    - In vuln build, tap “Expose PendingIntent (demo)” and observe a broadcast with action dev.jamescullimore.android_security_training.web.LEAK_PI carrying a mutable PendingIntent in extras.
    - Build a helper app or use instrumentation to capture that broadcast and invoke the PI, demonstrating hijack risk. Secure builds use FLAG_IMMUTABLE and don’t expose the PI.

6) Harden and retest (10–20 min)
    - WebView hardening (already present in SecureWebViewHelper for reference):
        - Disable JavaScript unless required (settings.javaScriptEnabled = false); if needed, narrowly scope and never expose secrets via addJavascriptInterface.
        - Block mixed content; disable file access; restrict navigation via WebViewClient.shouldOverrideUrlLoading with an allowlist for https and expected host/path.
    - Component hardening:
        - Keep receivers not exported; if export is necessary, protect with signature-level permissions and validate callers. Use setPackage() when sending internal broadcasts.
        - Use PendingIntent.FLAG_IMMUTABLE for PIs you share. Avoid exposing sensitive PIs.
    - Where to implement edits:
        - Code: SecureWebViewHelper.kt/VulnWebViewHelper.kt
        - Manifests: app/src/main/AndroidManifest.xml (secure defaults), app/src/vuln/AndroidManifest.xml (training overrides) — revert the overrides to harden vuln variant as an exercise.
    - Retest the same adb and in-app flows; confirm mitigations.

Concrete commands quick reference
- Start Web topic activity (if not launcher for your variant):
    - adb shell am start -n <app_id>/.WebActivity
- Send broadcast to exported receiver (vuln only):
    - adb shell am broadcast -a dev.jamescullimore.android_security_training.web.DEMO --es msg "test"
- Follow logs:
    - adb logcat | grep -i -E "WebDemo|WebView|LEAK_PI"

Expected outcomes
- Vulnerable build: JS bridge calls succeed and can exfiltrate strings; unvalidated URL loads proceed; exported receiver accepts your broadcasts; mutable PI can be hijacked.
- Secure build: JS either disabled or tightly scoped and not exposing secrets; URL loads restricted to https + allowlisted hosts; receiver not exported and broadcasts scoped; PI immutable and not leaked.

Homework
- Read: OWASP MASVS — Interaction with the Mobile Platform.
- Optional: Watch “Mobile Application Security Testing: WebView and Intents” (YouTube).
- Write a short note on three key takeaways for securing WebView and exposed components in your apps.

 checklist for trainers
- Allowed hosts default to lethalmaus.github.io (primary) and jamescullimore.dev (legacy) in SecureWebViewHelper; update to your domain if hosting content.
- Untrusted demo uses http://neverssl.com in vuln builds; secure builds block it. Optionally host a malicious page that calls Android.leakToken().
- PendingIntent demo is self-contained: tapping "Expose PendingIntent (demo)" in vuln builds sends LEAK_PI targeted to this app; DemoReceiver triggers it and shows toasts.


## Section 9: Android Multi-User Architecture (AAOS and Multi-User Devices)

This topic explores Android’s multi-user model, how UIDs are derived, the implications for data isolation, the criticality and deprecation of sharedUserId, and how to correctly scope access and storage. Client and AAOS (automotive) variants are supported via the new topic flavor users.

How to build and run:
- Select a variant that includes the users topic, e.g., clientSecureUsers, clientVulnUsers, autoSecureUsers, autoVulnUsers.
- Launch the app. You’ll see the Multi-User screen (MultiUserActivity) with actions to show runtime info, store per-user vs global tokens, and attempt cross-user access (for discussion).

Key code paths:
- UI (topic-specific): app/src/users/java/.../MultiUserActivity.kt
- Helper API (interface): app/src/main/java/.../multiuser/MultiUserHelper.kt
- Secure implementation: app/src/secure/java/.../multiuser/SecureMultiUserHelper.kt
    - Per-user scoping using ANDROID_ID as a suffix and EncryptedSharedPreferences.
    - Avoids external storage for sensitive data; no cross-user attempts.
- Vulnerable implementation: app/src/vuln/java/.../multiuser/VulnMultiUserHelper.kt
    - Plaintext tokens keyed globally and written to public Downloads (shared), illustrating leakage across users/profiles.
    - Naive cross-user path probing to demonstrate why sandboxing prevents it.
- Providers: provideMultiUserHelper() in app/src/secure|vuln/java/.../network/Provider.kt

Background: user IDs, app IDs, and UIDs
- On AOSP, a process UID is generally computed as uid = userId * 100000 + appId.
- Each user/profile has its own app data directory (/data/user/<userId>/<package>/...), isolating app state across users.
- Apps run under unique Linux UIDs per user; runtime permission grants apply to the calling UID.

Criticalness of sharedUserId
- sharedUserId historically allowed multiple apps (signed with the same certificate) to share the same Linux UID and permission grants.
- This is dangerous and has been deprecated; new apps should not use sharedUserId. It weakens isolation and complicates updates and rollback.
- For training, we discuss it conceptually in the README; we do not enable it in the sample app.

Permissions like INTERACT_ACROSS_USERS(_FULL)
- These are privileged/signature permissions reserved for system/privileged apps (e.g., on OEM/AAOS images). Third‑party apps cannot hold them.
- The vuln manifest declares them with tools:ignore for classroom discussion; they will not be granted on standard devices.
- Cross-user APIs require these permissions; attempts from this app demonstrate expected SecurityException/denial.

Multi-user data separation and correct scoping
- Secure pattern: Store per-user data in the app’s internal storage and include a per-user namespace (e.g., ANDROID_ID) for keys.
- Avoid global/external storage for sensitive information; external locations can be visible across users/profiles and over ADB/USB user grants.
- Be explicit when caching data that should NOT roam across users; use per-user identifiers in keys or directories.

AAOS and automotive notes
- AAOS commonly uses multi-user setups (driver vs guest vs profiles). Head units may have additional OEM/system apps and policies.
- Ensure features that use user data behave per-user and do not leak data between users (e.g., nav history, tokens, preferences).
- System-only permissions (INTERACT_ACROSS_USERS) may be available to OEM apps; third‑party apps should design for strict isolation.

Exercises (45–75 min)
1) Create and switch users (emulator or test device)
    - List users: adb shell pm list users
    - Create a user: adb shell pm create-user "TestUser"
    - Switch active user (if supported): adb shell am switch-user <userId>
    - Start activity as a specific user: adb shell am start --user <userId> -n <app_id>/.MultiUserActivity
2) Per-user vs global token
    - In user A, tap Save Per-User Token (secure) and Save GLOBAL Token (INSECURE demo).
    - Switch to user B and tap Load Per-User Token (secure) — expect not found; then Load GLOBAL Token — may be visible if shared storage is common across users on your device.
3) UID math and sandbox
    - Tap Show Runtime Info and note uid, userId, appId.
    - Discuss why direct path access to /data/user/<otherId>/... fails.
4) Cross-user access attempt
    - Enter another userId in the field and tap Try Cross-User Read — observe denial or failure message; relate to INTERACT_ACROSS_USERS requirements.
5) AAOS discussion
    - Discuss multi-user implications for automotive features (e.g., media accounts, navigation). Consider per-user storage and per-user permissions.

Best practices
- Do not use sharedUserId; design for per-user isolation.
- Scope encryption keys and preferences per user; use EncryptedSharedPreferences for secrets.
- Avoid external/public storage for sensitive data.
- Validate caller identity and user context for any exported components (see Section 4: Permissions).

 trainer checklist
- Prepare an emulator/device that supports multiple users (emulator typically supports pm create-user; some devices may restrict it).
- For AAOS, prepare a head unit or emulator image with multiple profiles to demonstrate switching.
- Decide whether to demonstrate global token visibility in external storage depending on your lab device’s behavior.

References
- Multi-user support: https://source.android.com/docs/core/multitenant/multi-user
- UserManager and profiles: https://developer.android.com/reference/android/os/UserManager
- App data directories: https://developer.android.com/training/data-storage/app-specific



## Section 10: Other Common App Risks (OWASP MASVS focus)

This topic surfaces common misconfigurations and risky defaults that often slip into apps: manifest flags (debuggable, allowBackup), cleartext usage, and unintentionally exported components. It maps to OWASP MASVS requirements around platform interaction, data storage, and network communication.

How to build and run:
- Select a variant that includes the risks topic, e.g., clientSecureRisks, clientVulnRisks, autoSecureRisks, autoVulnRisks.
- Launch the app. You’ll see the “Other Common App Risks” screen with actions:
    - Show App Config (debuggable, allowBackup, cleartext permitted)
    - Test Cleartext HTTP (neverssl.com)
    - List Exported Components (activities/services/providers/receivers)

Key code paths:
- UI (topic-specific): app/src/risks/java/.../RisksActivity.kt
- Topic manifest: app/src/risks/AndroidManifest.xml (sets RisksActivity as launcher)
- Existing vulnerable components used for demonstration:
    - Web topic receiver exported in vuln builds: app/src/vuln/AndroidManifest.xml
    - Permissions topic service/provider exported in vuln builds: app/src/vuln/AndroidManifest.xml
- Network Security Config (cleartext/user CAs): per clientVuln/autoVuln under src/<flavor>/res/xml/

What you’ll observe:
- Secure builds:
    - Cleartext typically blocked (Network Security Config disallows cleartext by default in secure client/auto).
    - Receiver/service/provider are not exported unless intentionally required and protected.
- Vulnerable builds:
    - Cleartext may succeed (Test Cleartext HTTP button) because vuln configs allow it.
    - Exported receiver/service/provider are discoverable via “List Exported Components” and are callable from adb/other apps.

Exercises (30–45 min):
1) Recon manifest flags
    - Tap “Show App Config”. Discuss:
        - debuggable: true on debug builds (acceptable for development); never set true on release.
        - allowBackup: true enables Auto Backup/adb backup; sensitive data could be included unless excluded. Prefer false or define backup_rules to exclude secrets.
        - cleartextTrafficPermitted: whether HTTP is allowed globally (NetworkSecurityPolicy).
    - Open the Merged Manifest view to see the resolved values per variant.
2) Cleartext demonstration
    - clientVulnRisks: Tap “Test Cleartext HTTP (neverssl.com)” — expect a successful HTTP 200.
    - clientSecureRisks: The request should fail due to cleartext being disallowed by policy.
    - Capture traffic with Wireshark to show plaintext content when allowed.
3) Exported components
    - Tap “List Exported Components”. Note exported items in vuln builds (receiver, service, provider).
    - Try to interact:
        - Broadcast (vuln): adb shell am broadcast -a dev.jamescullimore.android_security_training.web.DEMO --es msg "hello"
        - Start service (vuln perm topic): adb shell am startservice -n <app_id>/.perm.DemoService
        - Query provider (vuln perm topic): adb shell content query --uri content://<app_id>.demo/hello
    - Compare behavior on secure builds (should be blocked by non-exported and/or permission checks).

OWASP MASVS mapping (selected):
- MASVS-ARCH: Secure architecture and hardened build configuration (debuggable, backups, exported components).
- MASVS-NETWORK: No cleartext transmission; proper TLS usage; pinning (see Section 1).
- MASVS-PLATFORM: Secure use of platform features and permissions (components, app links, receivers).
- MASVS-STORAGE: Data at rest protection; avoid backups of secrets (see Section 6).

Real-world leak examples to discuss:
- Accidentally exported ContentProviders exposing SQLite data.
- Debuggable release builds enabling WebView remote debugging and heap dumps.
- allowBackup true with broad backup_rules exposing tokens in SharedPreferences.
- Cleartext endpoints left for staging/QA leaking credentials over Wi‑Fi.

Best practices and hardening checklist:
- Do not set android:debuggable in manifests; rely on build types (debug vs release). Ensure release is not debuggable.
- Set android:allowBackup="false" for apps handling sensitive data, or provide explicit backup_rules excluding secrets.
- Disallow cleartext in Network Security Config; require HTTPS everywhere.
- Keep components non-exported by default. If exported, protect with permissions (signature when possible) and validate callers.
- Use app links with autoVerify to avoid hijackable BROWSABLE flows (see Section 5).
- Automate static checks (lint/MobSF) in CI to detect risky flags and exported components.

AAOS notes:
- Head units may include OEM/system apps with broader privileges. Ensure your app doesn’t rely on allowBackup for migrations. Validate component exposure carefully in multi-user contexts (see Section 9).

 trainer checklist:
- Prepare Wireshark/mitmproxy to demonstrate cleartext vs TLS.
- Decide on policy for allowBackup during lab (toggle and demonstrate with adb backup on old API levels if available; modern Android limits adb backup for third-party apps).
- Optionally add a lint or Gradle task to fail builds if debuggable is detected in release.
