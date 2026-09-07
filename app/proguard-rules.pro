# Retrofit / Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.arno.showtracker.data.remote.**.model.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
