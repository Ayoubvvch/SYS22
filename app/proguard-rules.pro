# ProGuard Rules for SYS22

# Keep JavascriptInterface annotations so WebView bridge methods are preserved
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Gson serialization models
-keepclassmembers class com.acmods.sys22.model.** { *; }
-keep class com.acmods.sys22.model.** { *; }

# Keep AndroidX WebKit
-keep class androidx.webkit.** { *; }

# Keep Core Activities and Receivers
-keep class com.acmods.sys22.MainActivity { *; }
-keep class com.acmods.sys22.receiver.** { *; }
-keep class com.acmods.sys22.update.** { *; }
