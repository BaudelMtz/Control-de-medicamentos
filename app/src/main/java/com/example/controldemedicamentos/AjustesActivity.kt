package com.example.controldemedicamentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import conectarBaseDeDatos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjustesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        // Configuración Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Inicializar datos
        cargarDatosUsuario()
        auth = Firebase.auth

        // Botones
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { cerrarSesionCompleta() }
        findViewById<LinearLayout>(R.id.llBtnSincronizar).setOnClickListener {
            Toast.makeText(this, "Sincronizando...", Toast.LENGTH_SHORT).show()
            // Aquí podrías agregar lógica para refrescar datos si fuera necesario
        }
        findViewById<LinearLayout>(R.id.llBtnCambiarPassword).setOnClickListener { mostrarDialogoCambioPassword() }
    }

    private fun cargarDatosUsuario() {
        // 1. Obtenemos el correo guardado (este nunca falla porque lo guardamos en el login)
        val prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "Usuario") ?: ""

        // Ponemos el correo de inmediato
        findViewById<TextView>(R.id.tvAjustesCorreo).text = correo

        // 2. Consultamos el nombre real en la Base de Datos para que sea 100% confiable
        lifecycleScope.launch(Dispatchers.IO) {
            val con = conectarBaseDeDatos()
            try {
                // Buscamos el nombre en SQL usando el correo
                val sql = "SELECT nombre FROM Usuarios WHERE correo = ?"
                val stmt = con?.prepareStatement(sql)
                stmt?.setString(1, correo)
                val rs = stmt?.executeQuery()

                if (rs != null && rs.next()) {
                    val nombreReal = rs.getString("nombre")

                    // Actualizamos la UI en el hilo principal
                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.tvAjustesNombre).text = nombreReal
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                con?.close()
            }
        }
    }

    private fun mostrarDialogoCambioPassword() {
        val editText = EditText(this)
        editText.hint = "Nueva contraseña"

        AlertDialog.Builder(this)
            .setTitle("Cambiar Contraseña")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaPass = editText.text.toString()
                if (nuevaPass.isNotEmpty()) {
                    actualizarPasswordEnSQL(nuevaPass)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarPasswordEnSQL(nuevaPass: String) {
        val prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            val con = conectarBaseDeDatos()
            try {
                val sql = "UPDATE Usuarios SET contraseña = ? WHERE correo = ?"
                val stmt = con?.prepareStatement(sql)
                stmt?.setString(1, nuevaPass)
                stmt?.setString(2, correo)
                stmt?.executeUpdate()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AjustesActivity, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                con?.close()
            }
        }
    }

    private fun cerrarSesionCompleta() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            getSharedPreferences("SesionUsuario", MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("MisPreferencias", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}