# Jetpack Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    void *;
}
-keep class androidx.compose.runtime.Recomposer { *; }

# ViewModels (instantiated through ViewModelFactory — keep the hierarchy)
-keep class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Database *; }
-keep class * { @androidx.room.TypeConverter *; }

# DataStore

# Coil
-dontwarn coil.**

# Game Models (Keep for persistence/serialization)
-keep class com.kotonosora.todolist.data.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# JGraphT / apfloat
-dontwarn java.lang.management.**
-dontwarn org.apfloat.**
