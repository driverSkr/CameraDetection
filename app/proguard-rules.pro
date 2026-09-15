# BaseActivityVB/BaseFragmentVB read the concrete binding type from genericSuperclass.
# Keep the hierarchy and signatures, and the generated inflate methods called by name.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep,allowobfuscation class com.ethan.base.component.BaseActivityVB
-keep,allowobfuscation class * extends com.ethan.base.component.BaseActivityVB
-keep,allowobfuscation class com.ethan.base.component.BaseFragmentVB
-keep,allowobfuscation class * extends com.ethan.base.component.BaseFragmentVB
-keep class ** implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
}

# Small remote-config DTOs are instantiated by Gson, including nested optional fields.
-keep class com.ethan.firebaseAnalytics.config.remoteExtract.bean.** { *; }
