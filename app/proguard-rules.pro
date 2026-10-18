# Add project specific ProGuard rules here.

# Moshi & JSON models
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.example.model.** { *; }
-keep class com.example.model.*JsonAdapter { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

