package com.example.das_animales

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.HttpsURLConnection


enum class ProviderType {
    BASIC
}

class HomeActivity : AppCompatActivity() {
    // Declarar elementos de la UI como atributos de la clase
    private lateinit var emailTextView: TextView
    private lateinit var providerTextView: TextView
    private lateinit var logOutButton: Button
    private lateinit var sacarFotoButton: Button
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Elementos del layout XML
        emailTextView = findViewById(R.id.emailTextView)
        providerTextView = findViewById(R.id.providerTextView)
        logOutButton = findViewById(R.id.logOutButton)
        sacarFotoButton = findViewById(R.id.sacarFotoButton)

        // Setup
        val bundle = intent.extras
        val email = bundle?.getString("email")  // ? porque pueden no existir
        val provider = bundle?.getString("provider")
        setup(email?:"", provider?:"")

        // Inicializar takePictureLauncher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val bundle = result.data!!.extras
                //val elImageView = findViewById<ImageView>(R.id.imageView)
                val laminiatura = bundle?.get("data") as Bitmap?

                // Comprimir el Bitmap a formato PNG
                val stream = ByteArrayOutputStream()
                laminiatura?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val fototransformada = stream.toByteArray()

                // Convertir el array de bytes a una cadena Base64
                val fotoen64 = Base64.encodeToString(fototransformada, Base64.DEFAULT)

                // Crear una URL con parámetros usando Uri.Builder
                val pUsuario = email
                val pAnimal = "nombre_animal"  // TODO: Que el usuario introduzca el nombre

                val builder = Uri.Builder()
                    .appendQueryParameter("usuario", pUsuario)
                    .appendQueryParameter("animal", pAnimal)
                    .appendQueryParameter("foto", fotoen64)

                val parametrosURL = builder.build().encodedQuery

                // Enviar la imagen y los parámetros al servidor
                Thread {
                    try {
                        val url = URL("http://34.121.128.202:81/subirFoto.php")
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

                        // Manejar la respuesta del servidor si es necesario
                        Log.d("Respuesta del Servidor", response.toString())

                        outputStream.close()
                        inputStream.close()
                        connection.disconnect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Error", "Error en la conexión con el servidor", e)
                        runOnUiThread {
                            Toast.makeText(this, "Error al subir la foto", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            } else {
                Log.d("Foto Sacada", "No has sacado ninguna foto")
            }
        }


        // Capturar imagen desde la cámara
        sacarFotoButton.setOnClickListener {
            sacarFoto()
        }

        // Cargar imágenes del servidor
        cargarImagenes()
    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"
        emailTextView.text = email
        providerTextView.text = provider

        logOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            onBackPressed()     // Vuelve a la actividad anterior
        }
    }

    private fun sacarFoto() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION_CODE)
        } else {
            // Permiso ya concedido, se puede sacar la foto
            val elIntentFoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(elIntentFoto)
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION_CODE = 1001
    }

    private fun cargarImagenes() {
        /*
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        int responseCode = 0;
        try {
            responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Bitmap elBitmap = BitmapFactory.decodeStream(conn.getInputStream());
            }
        } catch (IOException e) {
            …
        }
        */

    }
}