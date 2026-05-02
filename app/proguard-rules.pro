# IronSide Connect - ProGuard rules
# Medical device software: preserve stack traces for post-market surveillance

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Room entities and DAOs
-keep class com.ironsidemedical.connect.data.local.database.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Preserve Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Preserve Gson models for audit log serialization
-keepclassmembers class com.ironsidemedical.connect.domain.model.** { *; }

# Remove debug-only code in release
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}
