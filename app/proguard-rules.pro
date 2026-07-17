# Compose
-dontwarn androidx.compose.**

# Kotlin
-dontwarn kotlin.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel *; }
-keep, allowobfuscation, allowshrinking class kotlin.coroutines.Continuation
-keep, allowobfuscation, allowshrinking interface kotlin.coroutines.Continuation

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# Keep R class
-keep class com.ofa.vpn.R { *; }
-keep class com.ofa.vpn.R$* { *; }

# Keep data models
-keep class com.ofa.vpn.data.model.** { *; }
