# kotlinx.serialization models used over the wire
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.eka.conversation.matrix.**$$serializer { *; }
-keepclassmembers class com.eka.conversation.matrix.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
