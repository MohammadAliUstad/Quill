package com.yugentech.quill.cloud.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import timber.log.Timber

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val cloudSyncRepository: CloudSyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        return try {
            val categoryResult = cloudSyncRepository.syncCategoriesToCloud()
            val bookResult = cloudSyncRepository.syncBooksToCloud()

            if (categoryResult.isSuccess && bookResult.isSuccess) {
                Result.success()
            } else {
                Timber.w("SyncWorker encountered an error, queuing for retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed catastrophically")
            Result.failure()
        }
    }
}