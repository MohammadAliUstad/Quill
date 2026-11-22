package com.yugentech.quill.quota.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.yugentech.quill.quota.model.QuotaData
import com.yugentech.quill.quota.model.QuotaFields
import com.yugentech.quill.quota.model.todayDateString
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class QuotaService(
    private val firestore: FirebaseFirestore
) {

    private fun quotaDocRef(userId: String) =
        firestore.collection("users").document(userId).collection("quota").document("daily")

    suspend fun fetchQuota(userId: String): QuotaData? {
        return try {
            val doc = quotaDocRef(userId).get().await()
            if (!doc.exists()) return null

            QuotaData(
                queriesUsed = doc.getLong(QuotaFields.QUERIES_USED)?.toInt() ?: 0,
                lastResetDate = doc.getString(QuotaFields.LAST_RESET_DATE)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch quota")
            null
        }
    }

    suspend fun initQuota(userId: String) {
        try {
            val data = mapOf(
                QuotaFields.QUERIES_USED to 0,
                QuotaFields.LAST_RESET_DATE to todayDateString()
            )
            quotaDocRef(userId).set(data).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to init quota")
        }
    }

    suspend fun resetQuota(userId: String) {
        try {
            val data = mapOf(
                QuotaFields.QUERIES_USED to 0,
                QuotaFields.LAST_RESET_DATE to todayDateString()
            )
            quotaDocRef(userId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset quota")
        }
    }

    suspend fun incrementUsage(userId: String, amount: Int = 1) {
        try {
            quotaDocRef(userId).update(
                QuotaFields.QUERIES_USED, FieldValue.increment(amount.toLong())
            ).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to increment quota")
        }
    }
}
