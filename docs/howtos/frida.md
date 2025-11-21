## Frida

- Install Frida: https://frida.re/docs/installation/
- Set up for Android (device/emulator and frida-server): https://frida.re/docs/android/
- Example scripts for this lab are in the `frida/` folder of this repo.

### Quick start (attach/spawn)
- List device processes (verify connection):
  ```
  frida-ps -U
  ```
- Spawn the secure build with a script (example: hook secure storage):
  ```
  frida -U -f dev.jamescullimore.android_security_training.secure -l frida/hook_secure_storage.js
  ```
  - Tip: add `--no-pause` to let the app run immediately after spawn.
- Attach to a running app instead of spawning (alternative):
  ```
  frida -U -n dev.jamescullimore.android_security_training.secure -l frida/hook_secure_storage.js
  ```
- Trace common libc calls (example: `open`) for the secure build:
  ```
  frida-trace -U -i open -N dev.jamescullimore.android_security_training.secure
  ```
  - Alternate syntax (attach by name on some versions):
    ```
    frida-trace -U -i open -n dev.jamescullimore.android_security_training.secure
    ```

### Notes
- Package IDs:
  - Secure variants: `dev.jamescullimore.android_security_training.secure`
  - Vulnerable variants: `dev.jamescullimore.android_security_training.vuln`
- Scripts live in the `frida/` directory (for example: `frida/hook_secure_storage.js`).
- If you don’t see classes/methods right after spawning, use `--no-pause` or interact with the target screen so code paths load.
- On Google APIs emulators, Frida works fine for app‑level hooking even if `adbd` runs as root but the app cannot `su` — this is expected (see Root section for details).
