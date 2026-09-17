# Retrofit / Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.georgearn.showtracker.data.remote.**.model.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
