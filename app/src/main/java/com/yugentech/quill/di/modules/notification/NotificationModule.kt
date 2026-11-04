package com.yugentech.quill.di.modules.notification

import com.yugentech.quill.notification.datastore.NotificationDataStore
import com.yugentech.quill.notification.reminders.NotificationScheduler
import com.yugentech.quill.notification.reminders.ReminderManager
import com.yugentech.quill.notification.viewmodel.NotificationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val notificationModule = module {
    // The reminder settings are kept in the "user" DataStore, where they've always been.
    single { NotificationDataStore(dataStore = get(named("user"))) }
    single { NotificationScheduler(context = get()) }
    // QuillApp calls onAppStart() at every start, which books the reminders from the settings.
    single { ReminderManager(context = get(), scheduler = get(), notificationDataStore = get(), bookDao = get()) }
    viewModel { NotificationViewModel(reminders = get(), notificationDataStore = get()) }
}
