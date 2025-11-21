# 1. Certificate pinning & HTTPS

![Pinning screen preview](../../../app/src/test/snapshots/images/__PinningScreenPreview.png)

#### Where in code
  - Interface: `app/src/main/java/.../network/NetworkHelper.kt`
  - Secure: `app/src/secure/java/.../network/SecureNetworkHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../network/VulnNetworkHelper.kt`
  - Network Security Config (secure): `app/src/secure/res/xml/network_security_config_client_secure.xml`

#### Lab guide (do this)
  - Quick setup for interception and emulator proxy: see [MITM proxy quick setup (mitmproxy + emulator)](../../howtos/mitmproxy.md).
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
