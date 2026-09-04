package com.yugentech.quill.quota.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.yugentech.quill.quota.model.QuotaData
import com.yugentech.quill.quota.model.QuotaFields
import com.yugentech.quill.quota.model.QuotaLimits
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
                queriesLimit = doc.getLong(QuotaFields.QUERIES_LIMIT)?.toInt() ?: QuotaLimits.FREE,
                lastResetDate = doc.getString(QuotaFields.LAST_RESET_DATE)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch quota for user: $userId")
            null
        }
    }

    suspend fun initQuota(userId: String, isPro: Boolean) {
        try {
            val data = mapOf(
                QuotaFields.QUERIES_USED to 0,
                QuotaFields.QUERIES_LIMIT to if (isPro) QuotaLimits.PRO else QuotaLimits.FREE,
                QuotaFields.LAST_RESET_DATE to todayDateString()
            )
            quotaDocRef(userId).set(data).await()
            Timber.d("Quota initialized for user: $userId isPro=$isPro")
        } catch (e: Exception) {
            Timber.e(e, "Failed to init quota for user: $userId")
        }
    }

    suspend fun resetQuota(userId: String) {
        try {
            val data = mapOf(
                QuotaFields.QUERIES_USED to 0,
                QuotaFields.LAST_RESET_DATE to todayDateString()
            )
            quotaDocRef(userId).set(data, SetOptions.merge()).await()
            Timber.d("Quota reset for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset quota for user: $userId")
        }
    }

    suspend fun incrementUsage(userId: String, amount: Int = 1) {
        try {
            quotaDocRef(userId).update(
                QuotaFields.QUERIES_USED, FieldValue.increment(amount.toLong())
            ).await()
            Timber.d("Quota incremented by $amount for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to increment quota for user: $userId")
        }
    }

    suspend fun updateLimit(userId: String, isPro: Boolean) {
        try {
            val limit = if (isPro) QuotaLimits.PRO else QuotaLimits.FREE
            quotaDocRef(userId).set(
                mapOf(
                    QuotaFields.QUERIES_LIMIT to limit
                ),
                SetOptions.merge()
            ).await()
            Timber.d("Quota limit updated for user: $userId isPro=$isPro limit=$limit")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update quota limit for user: $userId")
        }
    }
}
