# Filora R8 / ProGuard rules (release).
#
# Philosophy: keep the rule set MINIMAL. R8 full mode (android.enableR8.fullMode)
# plus the AAR-bundled consumer rules from Hilt, Room, WorkManager, Coroutines and
# Coil already cover the common reflection surfaces. We only add app-specific keeps
# that those consumer rules cannot know about, so the NFR-9.1 12 MB budget is not
# eroded by over-keeping.

# --- Crash deobfuscation ---------------------------------------------------
# Retain line numbers so Play Console stack traces stay readable, then hide the
# original source file name (the line table is all the deobfuscator needs).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Annotations / generics metadata needed by serialization, Hilt and Room.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# --- kotlinx.serialization (typed Compose nav routes) ----------------------
# The @Serializable route/model classes are looked up via a generated $$serializer.
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.appblish.filora.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the @Serializable classes themselves (route + domain payloads) so R8 does
# not strip the synthetic constructors the generated serializer calls.
-keep @kotlinx.serialization.Serializable class com.appblish.filora.** { *; }

# --- WorkManager workers ---------------------------------------------------
# These are instantiated by WorkManager's DEFAULT factory via reflection on the
# class name persisted in WorkData (the app does not use androidx.hilt:hilt-work).
# Without this keep, R8 renames/removes them and enqueued operations crash with
# "Could not instantiate ...Worker". Keep every ListenableWorker subclass and its
# (Context, WorkerParameters) constructor.
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Hilt @EntryPoint interfaces -------------------------------------------
# The workers above pull dependencies through @EntryPoint interfaces resolved at
# runtime via EntryPointAccessors. Keep the interface members so the generated
# accessor can bind them after obfuscation.
-keep @dagger.hilt.EntryPoint interface * { *; }

# --- Enums -----------------------------------------------------------------
# Defensive: keep the synthetic values()/valueOf() used when serializing the many
# domain enums (ConflictStrategy, FileOperationKind, ViewLayout, ...).
-keepclassmembers enum com.appblish.filora.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
