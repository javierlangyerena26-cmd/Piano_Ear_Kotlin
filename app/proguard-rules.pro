# Keep rules for OIDO. The app does not use reflection based serialization
# on the Android runtime (kotlinx.serialization uses generated serializers).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.oido.** {
    *** Companion;
}
-keepclasseswithmembers class com.oido.** {
    kotlinx.serialization.KSerializer serializer(...);
}
