package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.Worker
import androidx.work.WorkerParameters

class FirstWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID) ?: "unknown"
        Thread.sleep(3000)
        val outData = Data.Builder()
            .putString(OUTPUT_DATA_ID, "$id-FirstDone")
            .build()
        return Result.success(outData)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
