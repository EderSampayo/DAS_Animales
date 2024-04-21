package com.example.das_animales

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var email: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val imageUrls = mutableListOf<String>()
    private lateinit var animalNameDialog: AlertDialog
    private val DIALOG_ANIMAL_NAME_LAYOUT = R.layout.dialog_nombre_animal
    private var nombreAnimal = ""
    private lateinit var locationHelper: LocationHelper
    private val REQUEST_CODE_SELECT_LOCATION = 2001

    private var latitud: Double = 0.0
    private var longitud: Double = 0.0

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
        this.email = bundle?.getString("email").toString()
        val provider = bundle?.getString("provider")
        setup(email?:"", provider?:"")

        // Inicializar LocationHelper
        locationHelper = LocationHelper(this, this)

        // Inicializar AlertDialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_nombre_animal, null)
        val editTextAnimalName = dialogView.findViewById<EditText>(R.id.editTextAnimalName)

        animalNameDialog = AlertDialog.Builder(this)
            .setTitle("Introduce el nombre del animal")
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                val animalName = editTextAnimalName.text.toString()
                Log.d("Nombre del Animal", animalName)
                this.nombreAnimal = animalName

                // Lanzar la cámara después de obtener el nombre del animal y su ubicación
                obtenerUbicacion()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Inicializar takePictureLauncher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                if (this.nombreAnimal.isNotEmpty()) {
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
                    //val pAnimal = "nombre_animal"  // TODO: Que el usuario introduzca el nombre

                    val builder = Uri.Builder()
                        .appendQueryParameter("usuario", pUsuario)
                        .appendQueryParameter("animal", this.nombreAnimal)
                        .appendQueryParameter("foto", fotoen64)
                        .appendQueryParameter("latitud", this.latitud.toString())
                        .appendQueryParameter("longitud", this.longitud.toString())

                    val parametrosURL = builder.build().encodedQuery

                    Log.d("Parametros URL", parametrosURL.toString())

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

                            // Enviar mensaje FCM a todos los usuarios de la aplicación
                            notificarUsuarios(this.nombreAnimal)

                            outputStream.close()
                            inputStream.close()
                            connection.disconnect()

                            cargarImagenes()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("Error", "Error en la conexión con el servidor", e)
                            runOnUiThread {
                                Toast.makeText(this, "Error al subir la foto", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }.start()
                } else {
                    Toast.makeText(this, "Por favor, introduce el nombre del animal", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("Foto Sacada", "No has sacado ninguna foto")
            }
        }

        // Inicializar RecyclerView y adaptador
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(imageUrls)
        recyclerView.adapter = imageAdapter


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
        animalNameDialog.show()
    }

    private fun lanzarCamara() {
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
        Thread {
            try {
                val url = URL("http://34.121.128.202:81/obtenerFotos.php?usuario=${this.email}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.doOutput = true

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                // Aquí procesa la respuesta del servidor, en este ejemplo asumimos que el servidor devuelve una lista de URLs separadas por comas.
                val imageUrls = response.toString().split(",")

                Log.d("Image URLs", imageUrls.toString())

                // Actualizar el adaptador con las nuevas URLs de imágenes
                runOnUiThread {
                    imageAdapter.imageUrls.clear()
                    imageAdapter.imageUrls.addAll(imageUrls)
                    imageAdapter.notifyDataSetChanged()
                }

                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Error", "Error en la conexión con el servidor", e)
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Error al cargar las imágenes", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun notificarUsuarios(pAnimal: String) {
        Thread {
            try {
                val url = URL("http://34.121.128.202:81/notificarUsuarios.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val outputStream = connection.outputStream
                val postData = "animal=$pAnimal".toByteArray(Charsets.UTF_8)
                outputStream.write(postData)

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                Log.d("Respuesta del Servidor", response.toString())

                outputStream.close()
                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Error", "Error en la conexión con el servidor", e)
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Error al notificar usuarios", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private val obtenerUbicacion = {
        if (locationHelper.checkLocationPermission()) {
            locationHelper.getCurrentLocation { latitude, longitude ->
                // Actualizar las variables latitud y longitud
                this.latitud = latitude
                this.longitud = longitude

                // Continuar con la lógica de sacar la foto después de obtener la ubicación
                lanzarCamara()
            }
        } else {
            locationHelper.requestLocationPermission()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            locationHelper.getLocationPermissionCode() -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationHelper.getCurrentLocation { latitude, longitude ->
                        // Actualizar las variables latitud y longitud
                        this.latitud = latitude
                        this.longitud = longitude

                        // Continuar con la lógica de sacar la foto después de obtener la ubicación
                        lanzarCamara()
                    }
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    lanzarCamara()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}