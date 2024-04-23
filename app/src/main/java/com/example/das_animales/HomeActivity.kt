package com.example.das_animales

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


enum class ProviderType {
    BASIC
}

class HomeActivity : AppCompatActivity() {
    // Declarar elementos de la UI como atributos de la clase
    private lateinit var emailTextView: TextView
    private lateinit var logOutButton: Button
    private lateinit var sacarFotoButton: Button
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var email: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val imageUrls = mutableListOf<String>()
    private val animales = mutableListOf<String>()
    private lateinit var animalNameDialog: AlertDialog
    private val DIALOG_ANIMAL_NAME_LAYOUT = R.layout.dialog_nombre_animal
    private var nombreAnimal = ""
    private lateinit var locationHelper: LocationHelper
    private val REQUEST_CODE_SELECT_LOCATION = 2001

    private var latitud: Double = 0.0
    private var longitud: Double = 0.0

    private val imageLocationMap = mutableMapOf<String, Triple<Double, Double, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //probarDeleteDeMiContentProvider("http://34.121.128.202:81/uploads/IMG_20240422_152327_Pepe.jpg")

        // Configurar el AlarmManager
        //configurarAlarmManager()

        // Elementos del layout XML
        emailTextView = findViewById(R.id.emailTextView)
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

                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val nombreUri = "IMG_" + timeStamp + "_" + this.nombreAnimal + ".jpg"

                    val builder = Uri.Builder()
                        .appendQueryParameter("usuario", pUsuario)
                        .appendQueryParameter("animal", this.nombreAnimal)
                        .appendQueryParameter("foto", fotoen64)
                        .appendQueryParameter("latitud", this.latitud.toString())
                        .appendQueryParameter("longitud", this.longitud.toString())
                        .appendQueryParameter("nombreUri", nombreUri)

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

                            // Manejar la respuesta del servidor
                            Log.d("Respuesta del Servidor", response.toString())

                            // Insertar la imagen en el ContentProvider
                            val contentResolver = this.contentResolver
                            val uri = Uri.parse("content://com.example.das_animales.provider/datos")

                            val values = ContentValues().apply {
                                put("image_uri", "http://34.121.128.202:81/uploads/$nombreUri")
                            }

                            val nuevoUri = contentResolver?.insert(uri, values)

                            if (nuevoUri != null) {
                                Log.d("ContentProvider", "URI insertada correctamente: $nuevoUri")
                            } else {
                                Log.e("ContentProvider", "Error al insertar URI en ContentProvider")
                            }

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
                            Toast.makeText(this@HomeActivity, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                                cargarImagenes()
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
        imageAdapter = ImageAdapter(imageUrls, animales, this.email) { imageUrl ->
            val location = imageLocationMap[imageUrl]
            if (location != null) {
                val latitude = location.first
                val longitude = location.second
                // Abrir el Google Maps con la ubicación
                locationHelper.openGoogleMapsAtLocation(latitude, longitude)
            } else {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }
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

                Log.d("Respuesta del Servidor", response.toString())

                // Parsear el JSON
                val jsonArray = JSONArray(response.toString())
                val animales = mutableListOf<String>()
                val imageUrls = mutableListOf<String>()
                val latitudes = mutableListOf<Double>()
                val longitudes = mutableListOf<Double>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val animal = jsonObject.getString("animal")
                    val imageUrl = jsonObject.getString("foto")
                    val latitude = jsonObject.getDouble("latitud")
                    val longitude = jsonObject.getDouble("longitud")

                    animales.add(animal)
                    imageUrls.add(imageUrl)
                    latitudes.add(latitude)
                    longitudes.add(longitude)

                    // Almacenar en el HashMap
                    imageLocationMap[imageUrl] = Triple(latitude, longitude, animal)
                }

                Log.d("Animales", animales.toString())
                Log.d("Image URLs", imageUrls.toString())
                Log.d("Latitudes", latitudes.toString())
                Log.d("Longitudes", longitudes.toString())

                // Actualizar el adaptador con las nuevas URLs de imágenes
                runOnUiThread {
                    imageAdapter.imageUrls.clear()
                    imageAdapter.animales.clear()
                    imageAdapter.imageUrls.addAll(imageUrls)
                    imageAdapter.animales.addAll(animales)
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

    private fun probarDeleteDeMiContentProvider(pUri: String)
    {
        // Configurar el ContentResolver
        val contentResolver = this?.contentResolver

        // Construir la URI con el ID o la URI a eliminar
        val uriAEliminar = Uri.parse("content://com.example.miaplicacion.provider/datos/$pUri")

        // Llamar al método delete del ContentProvider
        val filasEliminadas = contentResolver?.delete(uriAEliminar, null, null)

        // Verificar si se eliminó la imagen correctamente
        if (filasEliminadas != null && filasEliminadas > 0) {
            Log.d("Delete", "Imagen eliminada correctamente")
        } else {
            Log.d("Delete", "No se ha podido eliminar la imagen")
        }
    }

    /*
    private fun configurarAlarmManager() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20) // Hora en formato 24 horas
            set(Calendar.MINUTE, 24)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    */
}