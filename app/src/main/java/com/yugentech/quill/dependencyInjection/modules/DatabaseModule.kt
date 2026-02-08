package com.yugentech.quill.dependencyInjection.modules

import androidx.room.Room
import com.yugentech.quill.room.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "quill_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    single {
        get<AppDatabase>().libraryDao()
    }

    single {
        get<AppDatabase>().catalogDao()
    }

    single {
        get<AppDatabase>().bookChapterDao()
    }
}