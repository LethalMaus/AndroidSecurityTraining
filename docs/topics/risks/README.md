# 10. Risk modeling & dangerous defaults

![Risks screen preview](../../../app/src/test/snapshots/images/__RisksScreenPreview.png)

#### Where in code
Topic `risks` demonstrates configurations that often introduce risk (e.g., allowing cleartext, trusting user CAs, broad intent filters).

#### Lab demo: Backup leaks (ADB backup + ABE)
Goal: Show how permissive backup defaults can leak app data from the vuln flavor when backups are enabled.

Pre‑reqs
- Build and install the vuln flavor (applicationId: dev.jamescullimore.android_security_training.vuln).
- The main manifest sets allowBackup=true and provides backup rules. This enables adb backup/Auto Backup on devices where supported.
- ADB backup is deprecated on modern Android, but still works on some images/APIs. If your device/emulator blocks adb backup, try an API 28–29 AVD without Google Play, or use a rooted device with bmgr. This lab is for demonstration only.

1) Generate some app data to leak (SharedPreferences)
- Launch the vuln app and open the Users or Risks topic.
- In Users, tap "Save Per‑User Token (secure)" (in vuln it writes a plaintext token to shared_prefs/tokens_plain.xml).
- Alternatively, use other screens to create files or preferences.

2) Create an ADB backup of the vuln app
Run on your host shell:
```
adb backup -f backup.ab dev.jamescullimore.android_security_training.vuln
```
- The device may prompt you to enter a backup password. Remember it if you set one. You can also leave it empty for an unencrypted .ab file on some images.
- If you see "Now unlock your device and confirm the backup operation", approve it on the emulator/device.

3) Unpack the .ab backup using ABE (Android Backup Extractor)
- We include abe.jar at the repo root for convenience.
- If you set a backup password, use it in the command below; otherwise leave the password argument empty.
```
java -jar abe.jar unpack backup.ab backup.tar <password>
```
Examples
- With password: `java -jar abe.jar unpack backup.ab backup.tar mypass`
- Without password (some ABE versions accept empty final arg): `java -jar abe.jar unpack backup.ab backup.tar ""`

4) Extract the tar and inspect files
```
tar -xvf backup.tar
```
- Look under `apps/dev.jamescullimore.android_security_training.vuln/` for shared_prefs, databases, files, etc.
- Example target for this lab:
  `apps/dev.jamescullimore.android_security_training.vuln/r/shared_prefs/tokens_plain.xml`

Troubleshooting
- If `adb: backup is not supported` or nothing is backed up:
  - Ensure you installed the vuln flavor and that allowBackup=true (see Risks → Show App Config).
  - Try an older emulator image (API 28–29, non‑Google Play). Some newer images disable adb backup.
  - Some OEM images require enabling Developer options → Full desktop backups.
- If ABE fails to unpack, try a different Java version (Java 8–17 typically work) or re‑run the backup without password.

Secure variant contrast
- A secure release should set allowBackup=false or provide strict data_extraction_rules/backup_rules that exclude secrets.
- Corporate builds may also enforce device policy to disable key/value or full backups.

#### Misconfigured Android Manifests (common mistakes)
- Unprotected exported components; broad intent-filters; missing permissions; wrong taskAffinity; debuggable in release.
- Inspect: `adb shell dumpsys package dev.jamescullimore.android_security_training.vuln | sed -n '1,120p'` (look for exported, permissions, usesCleartextTraffic)
- Also: Android Studio → Merged Manifest view (compare secure vs vuln flavors).
- Reading: https://developer.android.com/guide/topics/manifest/manifest-intro, MASVS-PLATFORM https://mas.owasp.org/MASVS/

#### Cleartext traffic problems
- Accidental HTTP, SDKs using HTTP, missing/overbroad NetworkSecurityConfig.
- Check policy: `adb shell dumpsys network_security_config`
- App check: Risks → "Test Cleartext HTTP" button.
- Reading: https://developer.android.com/training/articles/security-config

#### Debuggable build leakage
- Never ship debuggable=true. Risks: JDWP attach, run-as, bypass checks.
- Verify device build: `adb shell getprop ro.debuggable` (should be 0 on production images)
- Verify app: `adb shell dumpsys package dev.jamescullimore.android_security_training.vuln | grep -i debug`
- Reading: https://developer.android.com/studio/build/build-variants

#### FileProvider misuse
- Over-broad <paths>, missing intent flags, leaking temp files.
- Safer: narrow paths, set FLAG_GRANT_READ_URI_PERMISSION per-intent, revoke after share.
- Reading: https://developer.android.com/reference/androidx/core/content/FileProvider

#### External storage risks
- External storage is world-readable by other apps; don’t store secrets.
- Safer: `Context.MODE_PRIVATE`, EncryptedFile/Jetpack Security.
- Reading: https://developer.android.com/training/data-storage, https://developer.android.com/topic/security/data

#### OWASP MASVS alignment
- Storage, Crypto, Platform, Build, Resilience. Map your controls to MASVS.
- Reading: https://mas.owasp.org/MASVS/

#### Keystore and signing key safety
- Don’t store keystores or passwords in repo/CI. Prefer Play App Signing / hardware-backed.
- Use secret vaults, RBAC, approvals, audit.
- Reading: https://support.google.com/googleplay/android-developer/answer/9842756, https://developer.android.com/topic/security/data#keystore

#### Gitignore & preventing sensitive file leaks
- Ignore local.properties, *.jks/*.keystore, .env, build/, .gradle/.
- Use pre-commit hooks or scanners to block secrets.
- Reading: https://github.com/github/gitignore/blob/main/Android.gitignore

#### Outdated libraries & dependency risks
- Audit regularly; track CVEs in transitive deps.
- Tools: Gradle Versions Plugin, OWASP Dependency Check.
- Reading: https://github.com/ben-manes/gradle-versions-plugin, https://github.com/jeremylong/DependencyCheck

#### SQL Injection in Android apps
- Don’t concatenate SQL; use `?` parameters or Room.
- Validate input from Intents/deep links before DB use.
- Reading: https://developer.android.com/training/data-storage/room, https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase#query

#### StrandHogg (task hijacking)
- Avoid custom taskAffinity; validate session on resume; use FLAG_SECURE for sensitive flows.
- Reading: general guidance https://developer.android.com/guide/components/activities/tasks-and-back-stack

#### Tapjacking & TapTrap
- Overlay/animation-based tap hijack. Require explicit confirmation; filter obscured touches.
- Demo: Risks → Enable/Disable FLAG_SECURE.
- Reading: https://developer.android.com/reference/android/view/View#attr_android:filterTouchesWhenObscured, https://taptrap.click/

#### Pixnapping attack
- Side-channel pixel stealing from visible UI. Minimize on-screen secrets; consider FLAG_SECURE; keep patched.
- Reading: https://www.pixnapping.com/

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
  - https://github.com/nelenkov/android-backup-extractor
  - https://en.wikipedia.org/wiki/Log4Shell
