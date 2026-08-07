# ========== 基本混淆配置 ==========
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ========== 保持必要的类 ==========

# 保持主Activity
-keep class com.example.shijian2.MainActivity { *; }

# 保持数据模型类（用于序列化）
-keep class com.example.shijian2.data.** { *; }

# 保持UI组件类
-keep class com.example.shijian2.ui.** { *; }

# 保持服务类
-keep class com.example.shijian2.service.** { *; }

# ========== 保持第三方库必要的类 ==========

# 保持Compose相关类
-keep class androidx.compose.** { *; }

# 保持序列化相关类
-keep class kotlinx.serialization.** { *; }

# 保持DataStore相关类
-keep class androidx.datastore.** { *; }

# 保持WorkManager相关类
-keep class androidx.work.** { *; }

# ========== 保持必要的注解 ==========
-keepattributes *Annotation*

# ========== 保持资源ID ==========
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ========== 优化配置 ==========
-optimizationpasses 5