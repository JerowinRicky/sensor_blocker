# Security Lockdown Proguard / R8 Rules

# Preserve Room entities and DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep our domain data models & policies
-keep class com.sensorlockdown.app.lockdown.model.** { *; }
-keep class com.sensorlockdown.app.authentication.model.** { *; }
-keep class com.sensorlockdown.app.persistence.entity.** { *; }

# Security: Strip out debug logging statements in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Preserve DeviceAdminReceiver entrypoint
-keep public class com.sensorlockdown.app.devicepolicy.LockdownDeviceAdminReceiver {
    public <init>();
}

# Preserve BootCompletedReceiver
-keep public class com.sensorlockdown.app.receiver.BootReceiver {
    public <init>();
}

# Preserve ContentProvider for emergency IPC
-keep public class com.sensorlockdown.app.emergency.EmergencyRecoveryProvider {
    public <init>();
}

