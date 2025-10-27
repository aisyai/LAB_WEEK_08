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

    // Create an instance of a work manager
    private val workManager = WorkManager.getInstance(this)

    // Deklarasi WorkRequest di scope class agar bisa diakses oleh observer
    private lateinit var firstRequest: OneTimeWorkRequest
    private lateinit var secondRequest: OneTimeWorkRequest

    // ThirdRequest TIDAK dideklarasikan di sini karena akan dibuat ulang di startThirdWorkerChain()
    // agar bisa di-enqueue lagi.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Cek permission notifikasi di Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0xCA7)
            }
        }

        // Constraints yang sama digunakan untuk semua worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        // === 1. Buat Work Requests ===
        firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(constraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, "001"))
            .build()

        secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(constraints)
            .build()

        // === 2. Chain Worker Awal (HANYA FirstWorker & SecondWorker) ===
        // ThirdWorker TIDAK dimasukkan ke sini
        workManager
            .beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // === 3. Observer First Worker ===
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("✅ First process is done")
                }
            }

        // === 4. Observer Second Worker (TRIGGER NotificationService) ===
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    // Setelah SecondWorker selesai, jalankan NotificationService
                    showResult("✅ Second process is done")
                    launchNotificationService()
                }
            }

        // === 5. Observer NotificationService (TRIGGER ThirdWorker Chain) ===
        // NotificationService memberi tahu MainActivity jika proses 10 detiknya selesai
        NotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("🔔 NotificationService (channel $channelId) finished. Starting ThirdWorker chain...")
            // Panggil fungsi untuk memulai ThirdWorker dan SecondNotificationService
            startThirdWorkerChain()
        }

        // === 6. Observer SecondNotificationService (Service TERAKHIR) ===
        SecondNotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("🎆 SecondNotificationService (channel $channelId) is done! ALL TASKS COMPLETE.")
        }
    }

    // Fungsi untuk membuat input data bagi worker
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Ganti ke LONG agar terbaca
    }

    // Launch the NotificationService (Service 1)
    private fun launchNotificationService() {
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }

        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // Launch the SecondNotificationService (Service 2 - Dijalankan setelah ThirdWorker selesai)
    private fun launchSecondNotificationService() {
        val intent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, intent)
    }

    // Fungsi yang mengimplementasikan proses Worker TERAKHIR: ThirdWorker -> SecondNotificationService
    private fun startThirdWorkerChain() {
        // Constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        // Buat WorkRequest baru
        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(constraints)
            .build()

        // Enqueue ThirdWorker
        workManager
            .beginWith(thirdRequest)
            .enqueue()

        // Observer Third Worker (TRIGGER SecondNotificationService)
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("✅ Third process is done (Final Worker)")
                    // Panggil Service terakhir
                    launchSecondNotificationService()
                }
            }
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}