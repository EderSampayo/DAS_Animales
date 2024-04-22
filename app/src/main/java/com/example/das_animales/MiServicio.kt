package com.example.das_animales

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class MiServicio : IntentService("MiServicio") {
    override fun onHandleIntent(intent: Intent?) {
        // Código para la tarea o servicio a ejecutar
        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        // Crear el canal de notificación para Android Oreo y versiones posteriores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "animal_notificacion",
                "Animal Notificacion",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Crear la notificación
        val notificationBuilder = NotificationCompat.Builder(this, "animal_notificacion")
            .setContentTitle("Animales esperándote")
            .setContentText("¡Hay animales esperándote para que los conozcas!")
            .setAutoCancel(true) // La notificación se cierra al tocarla

        // Mostrar la notificación
        notificationManager.notify(1234, notificationBuilder.build())
    }
}