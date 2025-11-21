# 6. Secure storage

![Secure storage screen preview](../../../app/src/test/snapshots/images/__StorageScreenPreview.png)

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
