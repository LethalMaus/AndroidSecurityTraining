# 3. Reverse‑engineering resistance

![Reverse‑engineering screen preview](../../../app/src/test/snapshots/images/__REHomePreview.png)

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
