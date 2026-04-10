package com.yugentech.quill.di.modules.core

import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val firebaseModule = module {
    single {
        FirebaseAuth.getInstance()
    }

    single {
        FirebaseFirestore.getInstance()
    }

    single {
        Identity.getSignInClient(androidContext())
    }

    single {
        FirebaseFunctions.getInstance(
            app = FirebaseApp.getInstance(),
            regionOrCustomDomain = "us-central1"
        )
    }
}