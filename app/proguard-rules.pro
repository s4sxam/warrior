# kotlinx.serialization — keep all @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** *; }
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
@kotlinx.serialization.Serializable class ** { *; }