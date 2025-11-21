## Getting a rooted emulator (for certain labs)
To enable root access: Pick an emulator system image that is NOT labelled "Google Play". (The label text and other UI details vary by Android Studio version.)

Exception: As of 2020-10-08, the Release R "Android TV" system image will not run as root. Workaround: Use the Release Q (API level 29) Android TV system image instead.

Test it: Launch the emulator, then run `adb root`.

Command:
```
adb root
```
Expected output:
```
restarting adbd as root
```
or
```
adbd is already running as root
```
Not acceptable:
```
adbd cannot run as root in production builds
```

Alternate test:
```
adb shell
$ su
#
```
If the shell shows `#` after `su`, you have root. If it stays `$`, root is not available.

Steps: To install and use an emulator image that can run as root:

- In Android Studio, use the menu command Tools > AVD Manager.
- Click the + Create Virtual Device... button.
- Select the virtual Hardware, and click Next.
- Select a System Image.
- Pick any image that does NOT say "(Google Play)" in the Target column.
- If you depend on Google APIs (Google Sign In, Google Fit, etc.), pick an image marked with "(Google APIs)".
- You might have to switch from the "Recommended" group to the "x86 Images" or "Other Images" group to find one.
- Click the Download button if needed.
- Finish creating your new AVD.
- Tip: Start the AVD Name with the API level number so the list of Virtual Devices will sort by API level.
- Launch your new AVD. (You can click the green "play" triangle in the AVD window.)
