# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Kotlin Serialization (for @Serializable models in this module) ────────────
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.yugentech.quill.database.**$$serializer { *; }
-keepclassmembers class com.yugentech.quill.database.** {
    *** Companion;
}
-keepclasseswithmembers class com.yugentech.quill.database.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Type Converters ───────────────────────────────────────────────────────────
-keep @androidx.room.TypeConverters class * { *; }