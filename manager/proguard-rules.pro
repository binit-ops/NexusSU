# Keep Nexus Engine JNI Bridge (Prevents R8 from renaming C++ function names)
-keep class com.nexussu.manager.core.NexusEngine { *; }

# NEW: Keep Activities and Receivers launched via "am" from native code
-keep class com.nexussu.manager.SuRequestActivity { *; }
-keep class com.nexussu.manager.core.RootGrantedReceiver { *; }

# Keep Room Database Entities and DAOs
-keep class com.nexussu.manager.data.** { *; }

# Keep Kotlin Metadata (Required for Room to compile)
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep JSON parsing classes (For Update Checker)
-keep class org.json.** { *; }
-keep class com.nexussu.manager.core.UpdateChecker { *; }
