package fr.projetrenaissance.data.health

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.projetrenaissance.RenaissanceApplication
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class HealthConnectSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? RenaissanceApplication ?: return Result.failure()
        val preferences = application.container.preferences.preferences.first()
        val profileId = preferences.activeProfileId ?: return Result.success()
        if (!preferences.healthSyncEnabled || !preferences.healthBackgroundSyncEnabled) return Result.success()
        val report = application.container.healthData.sync(profileId)
        return if (report.errors.isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK = "renaissance-health-connect-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }
}
