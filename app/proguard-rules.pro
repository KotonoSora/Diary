# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------
# Kotlin
# -----------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    <fields>;
}

# -----------------------------------------------------------------------
# Hilt / Dagger
# -----------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.hilt.**

# -----------------------------------------------------------------------
# Room
# -----------------------------------------------------------------------
# Keep all Room entity, DAO, and database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-dontwarn androidx.room.**

# -----------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------
# Jetpack Navigation
# -----------------------------------------------------------------------
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# -----------------------------------------------------------------------
# WorkManager
# -----------------------------------------------------------------------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# -----------------------------------------------------------------------
# ViewModel / Lifecycle
# -----------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# -----------------------------------------------------------------------
# Native / JNI (NDK)
# -----------------------------------------------------------------------
# Keep all native methods so they are not stripped by ProGuard
-keepclasseswithmembernames class * {
    native <methods>;
}

# -----------------------------------------------------------------------
# App-specific classes
# -----------------------------------------------------------------------
# Keep all data/model classes used by Room and serialization
-keep class com.kotonosora.todolist.** { *; }

# -----------------------------------------------------------------------
# Miscellaneous
# -----------------------------------------------------------------------
# AndroidX Startup
-keep class androidx.startup.** { *; }
-dontwarn androidx.startup.**

# Suppress warnings for missing classes that are optional at runtime
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.**
