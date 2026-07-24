package com.yugentech.quill.di.modules.notification

import com.yugentech.quill.notification.NotificationHelper
import com.yugentech.quill.notification.ScheduledNotificationManager
import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.NotificationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationHelper(context = get()) }
    single { ScheduledNotificationManager(context = get()) }
    viewModel { NotificationViewModel(notificationManager = get(), userDataStore = get()) }
}
