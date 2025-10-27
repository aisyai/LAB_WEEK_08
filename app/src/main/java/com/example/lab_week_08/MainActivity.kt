package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {
    //Create an instance of a work manager
    //Work manager manages all your requests and workers
    //it also sets up the sequence for all your processes
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Permission buat notifikasi (Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // === WorkManager instance ===
        val workManager = WorkManager.getInstance(this)

        // === Constraints: hanya jalan kalau ada Internet & lagi charging ===
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val id = "001"

        // === First Worker ===
        val firstData = Data.Builder()
            .putString(FirstWorker.INPUT_DATA_ID, id)
            .build()

        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setInputData(firstData)
            .setConstraints(constraints)
            .build()

        // === Second Worker ===
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(constraints)
            .build()

        // === Third Worker ===
        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(constraints)
            .build()

        // === Chain semua Worker ===
        workManager
            .beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        // === Observers untuk masing-masing worker ===
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("✅ First process is done")
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("✅ Second process is done")
                    launchNotificationService()
                }
            }

        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("✅ Third process is done — All tasks finished!")
                    launchSecondNotificationService()
                }
            }

        // === Observe NotificationService completion (optional) ===
        NotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("🔔 NotificationService (channel $channelId) finished")
        }
    }


    //Build the data into the correct format before passing it to the worker as input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    //Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Launch the NotificationService
    private fun launchNotificationService() {
        //Observe if the service process is done or not
        //If it is, show a toast with the channel ID in it
        NotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the NotificationService
        //An ID of "001" is also passed as the notification channel ID
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }

        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }
    companion object{
        const val EXTRA_ID = "Id"
    }
}

private fun MainActivity.launchSecondNotificationService() {
    val intent = Intent(this, SecondNotificationService::class.java).apply {
        putExtra(SecondNotificationService.EXTRA_ID, "002")
    }
    ContextCompat.startForegroundService(this, intent)
}

