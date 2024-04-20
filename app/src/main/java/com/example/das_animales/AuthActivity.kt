package com.example.das_animales

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AuthActivity : AppCompatActivity() {
    // Declarar elementos de la UI como atributos de la clase
    private lateinit var signUpButton: Button
    private lateinit var loginButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Elementos del layout XML
        signUpButton = findViewById(R.id.signUpButton)
        loginButton = findViewById(R.id.loginButton)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)

        // Obtener permisos
        obtenerPermisos()

        // Obtener el token de FCM
        obtenerTokenFCM()

        // Setup
        setup()
    }

    private fun setup() {
        title = "Autenticación"

        signUpButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        enviarTokenAlServidor(emailEditText.text.toString())
                        navigateToHome(it.result?.user?.email ?: "", ProviderType.BASIC)    // Si no existe el email, se manda un string vacío, aunque nunca debería pasar por la comprobación de antes
                    } else {
                        showAlert()
                    }
                }
            }
        }

        loginButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        navigateToHome(it.result?.user?.email ?: "", ProviderType.BASIC)    // Si no existe el email, se manda un string vacío, aunque nunca debería pasar por la comprobación de antes
                    } else {
                        showAlert()
                    }
                }
            }
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error en la autenticación del usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun navigateToHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply{
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    private fun obtenerTokenFCM() {
        // Obtener el token de FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                this.token = task.result
                Log.d("FCM Token", this.token ?: "No se pudo obtener el token")

            } else {
                // Fallo al obtener el token
                Toast.makeText(this, "Error al obtener el token.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerPermisos() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                // PEDIR EL PERMISO
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
            }
        }

    }

    private fun enviarTokenAlServidor(email: String) {
        Thread {
            try {
                val url = URL("http://34.121.128.202:81/registrarToken.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                // Crear una URL con parámetros usando Uri.Builder
                val builder = Uri.Builder()
                    .appendQueryParameter("usuario", email)
                    .appendQueryParameter("token", token)

                val parametrosURL = builder.build().encodedQuery

                val outputStream = connection.outputStream
                outputStream.write(parametrosURL?.toByteArray())

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                // Loggear la respuesta del servidor
                Log.d("Registro de Token", response.toString())

                outputStream.close()
                inputStream.close()
                connection.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Error", "Error en la conexión con el servidor", e)
                runOnUiThread {
                    Toast.makeText(this, "Error al registrar el token", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}