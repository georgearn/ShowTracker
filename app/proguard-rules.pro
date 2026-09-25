# Keep Retrofit interface and DTO classes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod

# Keep all data remote DTOs and models (TMDB, OMDb)
-keep class com.georgearn.showtracker.data.remote.** { *; }
-keepclassmembers class com.georgearn.showtracker.data.remote.** { *; }

# Keep Moshi generated JsonAdapters
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(...);
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }

# Keep Retrofit interface methods
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**
