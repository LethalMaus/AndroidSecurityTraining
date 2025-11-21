# 9. Multi‑user/AAOS considerations

![Multi‑user screen preview](../../../app/src/test/snapshots/images/__MultiUserScreenPreview.png)

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
  adb install --user 10 app/build/outputs/apk/vulnUsers/debug/app-vuln-users-debug.apk
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
  - Expected on a rooted device (vuln build): You should see `[VULN][root-only demo]` output with a snippet of `/data/user/10/…/shared_prefs/tokens_plain.xml` if present. If not found, the app will report a clear not‑found message.

  Notes
  - These cross‑user operations use `su` and are intentionally provided only in the vulnerable flavor for teaching. The secure flavor shows the correct denial/limitations.
  - On some images, you may need to manually interact with the app under the secondary user once before the prefs path exists.

  G) Non‑rooted cross‑user demo (ADB shell as privileged actor)
  - You can clearly demonstrate the risk without root by using the `shell` UID (via ADB), which has cross‑user powers.
  - Ensure a secondary user exists and is started/unlocked:
    ```
    adb shell pm list users
    adb shell am start-user -w 10   # replace 10 with your userId
    ```
  - Make sure the app is installed for that user:
    ```
    adb shell cmd package install-existing --user 10 dev.jamescullimore.android_security_training.vuln
    ```
  - Cross‑user broadcast into the secondary user (vuln vs secure):
    ```
    adb shell am broadcast --user 10 -a dev.jamescullimore.android_security_training.DEMO
    ```
    Expected:
    - Vulnerable build: broadcast is delivered to the exported receiver.
    - Secure build: not delivered/blocked (receiver not exported or permission‑guarded).
  - Cross‑user provider query (if the vuln provider is exported):
    ```
    adb shell content query --user 10 --uri content://dev.jamescullimore.android_security_training.demo/some/path
    ```
    Expected:
    - Vulnerable build: returns a row (greeting) from the provider running under user 10.
    - Secure build: fails with SecurityException or no data.

  H) In‑app API attempts and expected SecurityException (stock devices)
  - Open the Users screen and set the target userId (e.g., 10).
  - Tap:
    - "Try sendBroadcastAsUser" → Expected: shows SecurityException because the app does not hold INTERACT_ACROSS_USERS(_FULL).
    - "Try createPackageContextAsUser" → Expected: shows SecurityException for the same reason.
  - This is by design on stock devices; it illustrates the platform restriction.

  I) Rooted device extras (vuln build only)
  - On rooted devices, the vuln helper will:
    - Attempt a cross‑user file read via `su` when you tap "Try Cross‑User File Read (root demo)".
    - Fall back to `su am broadcast --user <id> -a <action>` if the direct API is denied when you tap "Try sendBroadcastAsUser". The command result is shown in the UI.
  - Root does not grant your app INTERACT_ACROSS_USERS(_FULL), so createPackageContextAsUser will still fail unless the app is made privileged.

  J) Optional advanced: install as a privileged app to actually hold INTERACT_ACROSS_USERS_FULL
  - Use a userdebug/engineering image (or a device where you can remount /system). Then push the APK under /system/priv-app and whitelist the privileged permission.
  - Example (emulator/userdebug):
    ```
    adb root
    adb remount
    ./gradlew :app:assembleVulnUsersDebug
    adb push app/build/outputs/apk/vulnUsers/debug/app-vuln-users-debug.apk /system/priv-app/AndroidSecurityTraining/AndroidSecurityTraining.apk
    adb shell chmod 0644 /system/priv-app/AndroidSecurityTraining/AndroidSecurityTraining.apk
    # Whitelist the permission (file is included at repo root)
    adb push privapp-permissions-androidsecuritytraining.xml /system/etc/permissions/
    adb reboot
    ```
  - After reboot, open the Users screen and tap the two API attempts again:
    - sendBroadcastAsUser and createPackageContextAsUser should now succeed (no SecurityException) if the permissions are granted.

#### Troubleshooting (multi‑user demos)
- Confirm the target userId: `adb shell pm list users`.
- Start/unlock the target user: `adb shell am start-user -w <id>`.
- Ensure the app is installed for that user: `adb shell cmd package install-existing --user <id> <package>`.
- Use the correct package and action strings for your variant:
  - Package (vuln users): `dev.jamescullimore.android_security_training.vuln`
  - Broadcast action: `dev.jamescullimore.android_security_training.DEMO`
  - Provider authority (vuln): `content://dev.jamescullimore.android_security_training.demo/...`
- If rooted broadcast fallback fails: verify `su` works for the app and that the target user is started.

#### Best practices
  - Assume shared devices and multiple profiles (work, guest, automotive) — scope data to the active user.
  - Avoid cross‑profile leaks; respect enterprise restrictions and user separation.
  - Test on AAOS/automotive images where applicable.

#### Extra reading
  - Multi‑user support: https://developer.android.com/reference/android/os/UserManager
  - Android Automotive OS docs: https://developer.android.com/automotive
  - Work profile (Android Enterprise): https://developer.android.com/work
  - MASVS‑ARCH: https://mas.owasp.org/MASVS/
  - https://source.android.com/docs/devices/admin/multiuser-apps
