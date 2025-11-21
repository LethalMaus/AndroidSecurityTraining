# 2. End‑to‑end encryption (E2E)

![E2E screen preview](../../../app/src/test/snapshots/images/__E2EHomePreview.png)

#### Where in code
  - API surface: `app/src/main/java/.../crypto/CryptoHelper.kt`
  - Secure: `app/src/secure/java/.../crypto/SecureCryptoHelper.kt`
  - Vulnerable: `app/src/vuln/java/.../crypto/VulnCryptoHelper.kt`

#### Lab guide
  - Quick setup for interception and emulator proxy: see [MITM proxy quick setup (mitmproxy + emulator)](../../howtos/mitmproxy.md).
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

---

### ECB pattern‑leakage demo (vulnerable E2E)
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
