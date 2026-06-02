package com.yugentech.quill.aira.quick.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.quick.model.QuickActionPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class QuickActionService(
    private val functions: FirebaseFunctions
) {
    suspend fun getQuickActionResponse(payload: QuickActionPayload): String {
        return try {
            val result = functions
                .getHttpsCallable("quickAction")
                .call(payload.toMap())
                .await()

            (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from quick action function")
        } catch (e: Exception) {
            Timber.e(e, "QuickActionService call failed")
            throw e
        }
    }
}
