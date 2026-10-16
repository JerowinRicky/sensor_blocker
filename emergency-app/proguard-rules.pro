# Lockdown Emergency Proguard Rules
-keep class com.sensorlockdown.emergency.** { *; }

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

