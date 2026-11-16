# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data models used by Room, Moshi, and Cloud Firestore
-keep class com.example.data.model.** { *; }
-keep class com.example.data.model.social.** { *; }
-keep class com.example.data.db.** { *; }

# Keep Firebase and Google Play Services reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
