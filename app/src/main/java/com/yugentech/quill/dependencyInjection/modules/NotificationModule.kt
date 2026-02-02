package com.yugentech.quill.dependencyInjection.modules

import com.yugentech.quill.notifications.NotificationDataStore
import com.yugentech.quill.notifications.NotificationsViewModel
import com.yugentech.quill.notifications.active.ActiveNotificationManager
import com.yugentech.quill.notifications.NotificationService
import com.yugentech.quill.notifications.notificationRepository.NotificationRepository
import com.yugentech.quill.notifications.notificationRepository.NotificationRepositoryImpl
import com.yugentech.quill.notifications.scheduled.ReminderNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

// Koin module defining dependencies for the notification system
val notificationModule = module {

    // Initializes the notification service and creates channels immediately on app startup
    single(createdAtStart = true) {
        Timber.d("Initializing NotificationService and Channels")
        NotificationService(androidContext()).apply {
            createNotificationChannels()
        }
    }

    // Provides access to persistent notification settings
    single {
        NotificationDataStore(
            dataStore = get(named("notification"))
        )
    }

    // Manages the persistent foreground notification for active timers
    single {
        ActiveNotificationManager(
            context = androidContext()
        )
    }

    // Manages scheduling and cancelling of future alarm reminders
    single {
        ReminderNotificationManager(
            context = androidContext()
        )
    }

    // Repository that coordinates both active timer notifications and scheduled reminders
    single<NotificationRepository> {
        NotificationRepositoryImpl(
            activeNotificationManager = get(),
            reminderNotificationManager = get()
        )
    }

    // ViewModel for the notification settings screen
    viewModel {
        NotificationsViewModel(
            notificationRepository = get(),
            notificationDataStore = get()
        )
    }
}