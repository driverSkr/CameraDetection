# This file is referenced by prodVersion. Remove diagnostic output in production.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
