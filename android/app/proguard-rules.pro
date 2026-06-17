# Project-specific release shrinker rules.
# Keep Hilt, Room, Retrofit, and Gson metadata used by generated code/reflection.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses

# Hilt
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep @dagger.hilt.** class * { *; }
-keep @javax.inject.** class * { *; }

# Room Database
-keep class androidx.room.** { *; }
-keep @androidx.room.** class * { *; }
-keepclasseswithmembernames class * {
    @androidx.room.* <methods>;
}
-keepclasseswithmembernames class * {
    @androidx.room.* <fields>;
}

# Retrofit & Gson
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep @com.google.gson.annotations.SerializedName class * { *; }
-keepclasseswithmembernames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# App Models & DTOs
-keep class com.safenet.vpn.data.remote.dto.** { *; }
-keep class com.safenet.vpn.domain.model.** { *; }
-keep @com.google.gson.annotations.SerializedName class * { *; }

# AmneziaWG
-keep class com.zaneschepke.amneziawg.** { *; }
-keep interface com.zaneschepke.amneziawg.** { *; }

# Coroutines
-keepclasseswithmembernames class kotlinx.** {
    native <methods>;
}

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn kotlin.Metadata
-dontwarn com.google.common.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Remove logging in release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
