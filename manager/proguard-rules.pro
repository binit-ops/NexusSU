# Keep Nexus Engine JNI Bridge (Prevents R8 from renaming C++ function names)
-keep class com.nexussu.manager.core.NexusEngine { *; }

# Keep Room Database Entities and DAOs
-keep class com.nexussu.manager.data.** { *; }

# Keep Kotlin Metadata (Required for Room to compile)
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep JSON parsing classes (For Update Checker)
-keep class org.json.** { *; }
-keep class com.nexussu.manager.core.UpdateChecker { *; }
