package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {
    //In order to make the required notification, a service is required
    //to do the job for us in the foreground process

    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    //Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    /*This is used to bind a two-way communication
    In this tutorial, we will only be using a one-way communication
    therefore, the return can be set to null*/
    override fun onBind(intent: Intent?): IBinder? = null

    /*this is a callback and part of the life cycle
    the onCreate callback will be called when this service
    is created for the first time*/
    override fun onCreate() {
        super.onCreate()

        /*Create the notification with all of its contents and configurations
        in the startForegroundService() custom function*/
        notificationBuilder = startForegroundServiceNotification()

        /*Create the handler to control which thread the
        notification will be executed on.
        'HandlerThread' provides the different thread for the process to be executed on,
        while on the other hand, 'Handler' enqueues the process to HandlerThread to be executed.
        Here, we're instantiating a new HandlerThread called "SecondThread"
        then we pass that HandlerThread into the main Handler called serviceHandler*/
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundServiceNotification(): NotificationCompat.Builder {

        /*Create a pending Intent which is used to be executed
        when the user clicks the notification
        A pending Intent is the same as a regular Intent,
        The difference is that pending Intent will be
        executed "Later On" and not "Immediately"*/
        val pendingIntent = getPendingIntent()

        /*To make a notification, you should know the keyword 'channel'
        Notification uses channels that'll be used to
        set up the required configurations*/
        val channelId = createNotificationChannel()

        /*Combine both the pending Intent and the channel
        into a notification builder
        Remember that getNotificationBuilder() is not a built-in function!*/
        val builder = getNotificationBuilder(pendingIntent, channelId)

        /*After all has been set and the notification builder is ready,
        start the foreground service and the notification
        will appear on the user's device*/
        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    /*A pending Intent is the Intent used to be executed
    when the user clicks the notification*/
    private fun getPendingIntent(): PendingIntent {

        /*In order to create a pending Intent, a Flag is needed
        A flag basically controls whether the Intent can be modified or not later on
        Unfortunately Flag exists only for API 31 and above,
        therefore we need to check for the SDK version of the device first
        "Build.VERSION_CODES.S" stands for 'S' which is the API 31 release name*/
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0

        /*val intent = Intent(this, MainActivity::class.java)*/
        /*return PendingIntent.getActivity(this, 0, intent, flag)*/

        /*Here, we're setting MainActivity into the pending Intent
        When the user clicks the notification, they will be
        redirected to the Main Activity of the app*/
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, channelPriority)
            val service = ContextCompat.getSystemService(this, NotificationManager::class.java)
            service?.createNotificationChannel(channel)
            return channelId
        }
        return ""
    }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID) ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            try { Thread.sleep(3000L) } catch (e: InterruptedException) { /* ignore */ }
            notificationBuilder.setContentText("$i seconds until last warning").setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
