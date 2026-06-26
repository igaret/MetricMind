# SQLCipher
-keep class net.zetetic.database.** { *; }
-keep interface net.zetetic.database.** { *; }

# Room (generated)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-dontwarn com.google.errorprone.annotations.**

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Keep enum names used for Room TypeConverters
-keepclassmembers enum * { *; }

# Kotlinx coroutines
-dontwarn kotlinx.coroutines.**
