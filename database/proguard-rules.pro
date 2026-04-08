-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.yugentech.quill.database.**$$serializer { *; }
-keepclassmembers class com.yugentech.quill.database.** {
    *** Companion;
}
-keepclasseswithmembers class com.yugentech.quill.database.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep @androidx.room.TypeConverters class * { *; }