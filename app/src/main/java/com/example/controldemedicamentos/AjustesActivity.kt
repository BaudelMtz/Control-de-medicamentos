package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton

class AjustesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        // Configuración del Toolbar (Botón de retroceso)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- LÓGICA DE CERRAR SESIÓN ---
        val btnCerrarSesion: MaterialButton = findViewById(R.id.btnCerrarSesion)

        btnCerrarSesion.setOnClickListener {
            // 1. Limpiar los datos guardados de la sesión
            val sharedPref = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear() // Borra todos los datos del usuario activo
                apply()
            }

            // 2. Preparar el viaje al Login
            val intent = Intent(this, LoginActivity::class.java)

            // 3. Estas banderas destruyen el historial para que no pueda volver con el botón "Atrás"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // 4. Ejecutar la acción
            startActivity(intent)
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}