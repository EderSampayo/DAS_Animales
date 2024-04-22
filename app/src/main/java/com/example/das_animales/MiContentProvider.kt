package com.example.das_animales

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MiContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.example.das_animales.provider"
        private val CONTENT_URI = Uri.parse("content://$AUTHORITY/datos")

        private const val DATABASE_NAME = "image_database"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "images"

        private const val COLUMN_ID = "_id"
        private const val COLUMN_IMAGE_URI = "image_uri"
    }

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase

        // Obtener la URI de la imagen de los valores
        val uriImagen = values?.getAsString(COLUMN_IMAGE_URI)

        // Verificar si la imagen ya existe en la base de datos
        val cursor = db.query(TABLE_NAME, null, "$COLUMN_IMAGE_URI=?", arrayOf(uriImagen), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            // Si la imagen ya existe, retornar la URI correspondiente
            return Uri.parse("$CONTENT_URI/$uriImagen")
        }

        // Insertar la imagen en la base de datos
        val contentValues = ContentValues().apply {
            put(COLUMN_IMAGE_URI, "http://34.121.128.202:81/uploads/$uriImagen")
        }

        val id = db.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)

        if (id > 0) {
            // Devolver la URI que se utilizó como clave primaria
            return Uri.parse("$CONTENT_URI/$uriImagen")
        } else {
            throw SQLException("Error al insertar la imagen en la BBDD")
        }
    }


    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        // No hacer nada y devolver 0
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // URI completa a enviar al servidor
        val uriImagenAEliminar = "http://34.121.128.202:81/uploads/${uri.lastPathSegment}"

        val builder = Uri.Builder()
            .appendQueryParameter("nombreUri", uriImagenAEliminar)

        val parametrosURL = builder.build().encodedQuery

        Log.d("URI a eliminar", uriImagenAEliminar)

        // Enviar la imagen y los parámetros al servidor
        Thread {
            try {
                val url = URL("http://34.121.128.202:81/eliminarFotoDadaUri.php")
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

                // Respuesta del servidor
                Log.d("Respuesta del Servidor - Delete", response.toString())

                inputStream.close()

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Delete Error", "Error en el thread al eliminar la imagen", e)
            }
        }.start()

        // Eliminar la imagen de la base de datos
        val db = dbHelper.writableDatabase
        return db.delete(TABLE_NAME, selection, selectionArgs)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_IMAGE_URI TEXT PRIMARY KEY)"
            db.execSQL(CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }
}
