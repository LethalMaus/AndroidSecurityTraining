# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Reverse Engineering Lab notes:
# - To try obfuscation, enable minify for release in app/build.gradle.kts (isMinifyEnabled = true).
# - Keep Compose runtime and generated classes; otherwise, obfuscation may break previews and runtime.

# Compose keep rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin metadata (helps reflection and some tooling)
-keepclassmembers class kotlin.Metadata { *; }

# Keep model classes used via reflection (adjust as needed for your app)
-keep class dev.jamescullimore.android_security_training.** { *; }

# OkHttp/Okio warnings
-dontwarn okhttp3.**
-dontwarn okio.**

# If you add dynamic features and DexClassLoader demos, you might need to keep demo class names
# -keep class dev.training.dynamic.** { *; }

# WebView JS example (uncomment and customize when needed)
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi