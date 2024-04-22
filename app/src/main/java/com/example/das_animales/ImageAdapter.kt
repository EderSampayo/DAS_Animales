package com.example.das_animales

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ImageAdapter(var imageUrls: MutableList<String>, var animales: MutableList<String>, var email: String, private val locationClickListener: (String) -> Unit) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        val animal = animales[position]

        // Agregar esquema http si no está presente
        val fullUrl = if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            "http://$imageUrl"
        } else {
            imageUrl
        }

        Picasso.get().load(fullUrl).into(holder.imageView)
        holder.nombreAnimal.text = animal
        holder.obtenerUbicacionButton.setOnClickListener {
            locationClickListener(imageUrl)
        }
        holder.eliminarFotoButton.setOnClickListener {
            val uriSeleccionada = imageUrls[position]
            val esImagenDelUsuario = esImagenDeUsuario(uriSeleccionada, email)
            Log.d("Es imagen del usuario", email + " " + uriSeleccionada + " " + esImagenDelUsuario)

            if (esImagenDelUsuario) {

                // Construir la URI para eliminar el registro del ContentProvider
                val contentUri = Uri.withAppendedPath(MiContentProvider.CONTENT_URI, uriSeleccionada.substringAfterLast("/"))

                // Eliminar del ContentProvider
                val rowsDeleted = it.context.contentResolver.delete(contentUri, null, null)

                if (rowsDeleted > 0) {
                    Toast.makeText(it.context, "Tu foto ha sido eliminada correctamente", Toast.LENGTH_SHORT).show()
                    Log.d("ContentProvider", "Registro eliminado correctamente")
                } else {
                    Toast.makeText(it.context, "Se ha eliminado la foto del servidor", Toast.LENGTH_SHORT).show()
                    Log.d("ContentProvider", "No se pudo eliminar el registro del ContentProvider, pero sí del servidor")
                }

                //Eliminar de los datos locales
                imageUrls.removeAt(position)
                animales.removeAt(position)
                notifyDataSetChanged()
            } else {
                Toast.makeText(it.context, "No puedes borrar una foto que no es tuya", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    private fun esImagenDeUsuario(uriImagen: String, usuario: String): Boolean {
        var esImagenDelUsuario = false
        val builder = Uri.Builder()
            .appendQueryParameter("uriImagen", uriImagen)
            .appendQueryParameter("usuario", usuario)

        val parametrosURL = builder.build().encodedQuery

        val thread = Thread {
            try {
                val url = URL("http://34.121.128.202:81/esImagenDeUsuario.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val outputStream = connection.outputStream
                outputStream.write(parametrosURL?.toByteArray())

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                Log.d("Respuesta del Servidor", response.toString())

                if (response.toString().trim() == "true") {
                    esImagenDelUsuario = true
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Error", "Error en la conexión con el servidor", e)
            }
        }

        thread.start()
        thread.join() // Esperar a que el hilo termine

        return esImagenDelUsuario
    }


    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val obtenerUbicacionButton: Button = itemView.findViewById(R.id.obtenerUbicacionButton)
        val nombreAnimal: TextView = itemView.findViewById(R.id.nombreAnimal)
        val eliminarFotoButton: Button = itemView.findViewById(R.id.eliminarFotoButton)
    }
}
