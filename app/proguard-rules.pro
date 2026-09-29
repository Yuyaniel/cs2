# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.cs2.tracker.**$$serializer { *; }
-keepclassmembers class com.cs2.tracker.** { *** Companion; }
-keepclasseswithmembers class com.cs2.tracker.** { kotlinx.serialization.KSerializer serializer(...); }
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
