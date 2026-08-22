-keepattributes *Annotation*, InnerClasses
-keepattributes Signature, Exceptions

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# pdfbox-android references an optional JPEG 2000 decoder we do not ship.
-dontwarn com.gemalto.jp2.**

-keep class androidx.media3.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class dagger.hilt.** { *; }

# Retrofit interfaces and their kotlinx.serialization payloads are reflected over at runtime.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowobfuscation interface <1>

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
