package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class RecuperarContrasenaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Esto es lo que conecta este código con tu diseño bonito
        setContentView(R.layout.activity_recuperar_contrasena)

        // 1. Enlazamos el texto que funciona como botón de regreso
        val tvVolverInicio = findViewById<TextView>(R.id.tv_volver_inicio)

        // 2. Le decimos qué hacer cuando lo toquen
        tvVolverInicio.setOnClickListener {
            // 3. Preparamos el viaje hacia el LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 4. Cerramos esta pantalla para que no consuma memoria
            finish()
        }
    }
}