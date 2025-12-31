package com.yugentech.quill.dependencyInjection.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.themeDataStore:
        DataStore<Preferences> by preferencesDataStore(name = "theme")

private val Context.userDataStore:
        DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

val dataStoreModule = module {

    single<DataStore<Preferences>>(named("theme")) {
        androidContext().themeDataStore
    }

    single<DataStore<Preferences>>(named("user")) {
        androidContext().userDataStore
    }
}