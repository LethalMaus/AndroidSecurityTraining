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
