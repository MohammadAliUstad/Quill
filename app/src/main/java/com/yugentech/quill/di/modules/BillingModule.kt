package com.yugentech.quill.di.modules

import com.yugentech.quill.billing.service.BillingService
import com.yugentech.quill.billing.repository.BillingRepositoryImpl
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.quill.quota.QuotaRepositoryImpl
import com.yugentech.quill.quota.QuotaService
import com.yugentech.quill.quota.GlobalSyncManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val billingModule = module {
    single {
        BillingService(
            androidContext()
        )
    }

    single<BillingRepository> {
        BillingRepositoryImpl(
            get()
        )
    }

    single {
        QuotaService(
            firestore = get()
        )
    }

    single<QuotaRepository> {
        QuotaRepositoryImpl(
            authRepository = get(),
            quotaService = get(),
            quotaDao = get()
        )
    }

    single(createdAtStart = true) {
        GlobalSyncManager(
            authRepository = get(),
            userRepository = get(),
            quotaRepository = get(),
            billingRepository = get()
        )
    }

    viewModel {
        SubscriptionViewModel(
            billingRepository = get(),
            authRepository = get(),
            userRepository = get(),
            bookRepository = get()
        )
    }
}