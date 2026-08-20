package com.qubeguard.app.ml

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ModelUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloader: ModelDownloader,
    private val classifier: TransformerUrlClassifier
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val updated = downloader.updateModel()
        if (updated) {
            classifier.close()
            classifier.load()
            return Result.success()
        }
        return if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
