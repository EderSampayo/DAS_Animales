package com.example.das_animales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        //Iniciar el servicio o tarea a ejecutar
        val serviceIntent = Intent(context, MiServicio::class.java)
        context?.startService(serviceIntent)
    }
}