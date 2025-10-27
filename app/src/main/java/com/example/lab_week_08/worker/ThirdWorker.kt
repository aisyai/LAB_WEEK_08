package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThirdWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prev = inputData.getString(SecondWorker.OUTPUT_DATA_ID) ?: "noPrev"
        Thread.sleep(2000)
        val outData = Data.Builder()
            .putString(OUTPUT_DATA_ID, "$prev-ThirdDone")
            .build()
        return Result.success(outData)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
