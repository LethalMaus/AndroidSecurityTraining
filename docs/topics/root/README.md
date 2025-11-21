# 7. Root/Jailbreak detection

![Root detection screen preview](../../../app/src/test/snapshots/images/__RootScreenPreview.png)

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
  - Symptoms:
    - `which su` in `adb shell` returns a path (e.g., `/system/xbin/su`), but the app’s `can execute su` signal fails with "permission denied".
    - `su binary present` and `su in PATH` show true, yet elevation attempts fail.
  - Why: The emulator’s root is limited to the adb daemon; normal app UIDs cannot spawn a root shell. Different from a truly rooted environment (Magisk/KernelSU), where a superuser manager can grant per-app root.
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
