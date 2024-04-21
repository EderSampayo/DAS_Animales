package com.example.das_animales

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FotosWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Actualizar todos los widgets
        for (appWidgetId in appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun actualizarWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Thread {
            try {
                val url = "http://34.121.128.202:81/cantidadFotos.php"
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                val inputStream = urlConnection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                bufferedReader.close()
                inputStream.close()
                urlConnection.disconnect()

                val jsonResponse = JSONObject(response.toString())
                val cantidadFotos = jsonResponse.getInt("cantidad")

                // Actualizar el widget con la nueva cantidad de fotos en el hilo principal
                Handler(Looper.getMainLooper()).post {
                    val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                    remoteViews.setTextViewText(R.id.tvCantidadFotos, "Animales registrados en la aplicación: $cantidadFotos")

                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Mensaje de error en el widget
                Handler(Looper.getMainLooper()).post {
                    val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                    remoteViews.setTextViewText(R.id.tvCantidadFotos, "Error al obtener la cantidad de animales")

                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }.start()
    }
}