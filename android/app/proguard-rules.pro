# Moni release keep rules.
# UniFFI 生成绑定与 JNA/JNI 桥接不应被混淆，否则 release 包可能无法加载 Rust 核心。
-keep class uniffi.moni_core.** { *; }
-keep class com.sun.jna.** { *; }
-keep class com.agguy.moni.core.** { *; }

# kotlinx.serialization 生成的 serializer 通过静态字段/方法访问，保留模型序列化入口。
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class **$Companion { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# JNA 包含桌面 AWT 兼容分支，Android 运行时不会使用这些类。
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
