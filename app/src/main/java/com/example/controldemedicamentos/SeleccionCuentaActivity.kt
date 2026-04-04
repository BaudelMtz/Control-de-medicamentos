package com.example.controldemedicamentos

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

        // El layout activity_seleccion_cuenta.xml tiene un botón al final
        // Sin ID en el XML proporcionado, pero lo buscaré o sugeriré añadir uno
        val btnBack: Button? = findViewById(android.R.id.button1) // Placeholder o añadir ID
        // Como el XML no tiene ID para el botón de volver, actualizaré el XML primero.
    }
}