# Vosk & JNA rules
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn java.awt.**

# Coroutines & Hilt
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
