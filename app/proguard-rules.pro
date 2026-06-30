# Filora ProGuard/R8 rules.
# Kotlinx Serialization (typed nav routes) — keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.appblish.filora.** {
    kotlinx.serialization.KSerializer serializer(...);
}
