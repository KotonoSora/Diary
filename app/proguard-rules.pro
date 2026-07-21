# Jetpack Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    void *;
}
-keep class androidx.compose.runtime.Recomposer { *; }

# Hilt
-keep class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Database *; }
-keep class * { @androidx.room.TypeConverter *; }

# DataStore

# Retrofit
-keepattributes Signature, InnerClasses, AnnotationDefault
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, AnnotationDefault
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Coil
-dontwarn coil.**

# Google Play Billing
-dontwarn com.android.billingclient.api.**


# Game Models (Keep for persistence/serialization)
-keep class com.kotonosora.todolist.data.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**
