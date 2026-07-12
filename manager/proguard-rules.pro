# Keep Nexus Engine JNI Bridge
-keep class com.nexussu.manager.core.NexusEngine { *; }

# Keep Room Database Entities and DAOs
-keep class com.nexussu.manager.data.* { *; }

# Keep Kotlin Metadata (Required for Room)
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
