# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable data classes and their serializers in the app package.
-keep,includedescriptorclasses class com.zhongkao.yuwen.**$$serializer { *; }
-keepclassmembers class com.zhongkao.yuwen.** {
    *** Companion;
}
-keepclasseswithmembers class com.zhongkao.yuwen.** {
    kotlinx.serialization.KSerializer serializer(...);
}
