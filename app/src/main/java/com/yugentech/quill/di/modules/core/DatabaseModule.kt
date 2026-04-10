package com.yugentech.quill.di.modules.core

import androidx.room.Room
import com.yugentech.quill.database.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "quill_database"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single {
        get<AppDatabase>().airaMessageDao()
    }

    single {
        get<AppDatabase>().bookDao()
    }

    single {
        get<AppDatabase>().categoryDao()
    }

    single {
        get<AppDatabase>().catalogDao()
    }

    single {
        get<AppDatabase>().bookChunkDao()
    }

    single {
        get<AppDatabase>().categoryCacheDao()
    }

    single {
        get<AppDatabase>().userDao()
    }

    single {
        get<AppDatabase>().readingSessionDao()
    }

    single {
        get<AppDatabase>().quotaDao()
    }

    single {
        get<AppDatabase>().bookIndexingStateDao()
    }
}