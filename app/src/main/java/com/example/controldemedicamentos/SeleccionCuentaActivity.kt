package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SeleccionCuentaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seleccion_cuenta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Enlazamos el botón verde con su nombre correcto
        // OJO: Asegúrate de que en tu XML el ID siga siendo "btn_volver_login" (o cámbialo a "btn_volver_inicio_sesion" aquí y en el XML)
        val btnVolverInicioSesion = findViewById<Button>(R.id.btn_volver_login)

        // 2. Le damos la acción para que viaje a la pantalla de inicio
        btnVolverInicioSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 3. Cerramos esta pantalla
            finish()
        }
    }
}