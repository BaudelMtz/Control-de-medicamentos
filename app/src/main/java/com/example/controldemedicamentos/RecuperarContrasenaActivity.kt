package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RecuperarContrasenaActivity : AppCompatActivity() {

    // Variable para saber si ya pasó el captcha
    private var captchaVerificado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        val tvVolverInicio = findViewById<TextView>(R.id.tv_volver_inicio)
        val btnEnviar = findViewById<Button>(R.id.btn_enviar)
        val btnVerify = findViewById<Button>(R.id.btn_verify_captcha)
        val etCorreo = findViewById<EditText>(R.id.et_correo)

        // Botón para volver al Login
        tvVolverInicio.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Botón VERIFY (Simulación del CAPTCHA)
        btnVerify.setOnClickListener {
            // Aquí simulamos que al picarle, se verifica
            captchaVerificado = true
            Toast.makeText(this, "CAPTCHA Verificado correctamente", Toast.LENGTH_SHORT).show()
            // Opcional: podrías cambiar el color del botón a gris para indicar que ya se hizo
            btnVerify.isEnabled = false
        }

        // Botón Enviar Correo (Validación final)
        btnEnviar.setOnClickListener {
            val correoIngresado = etCorreo.text.toString().trim()

            if (correoIngresado.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo primero", Toast.LENGTH_SHORT).show()
            } else if (!captchaVerificado) {
                Toast.makeText(this, "Por favor, completa el CAPTCHA (botón VERIFY)", Toast.LENGTH_SHORT).show()
            } else {
                // Si todo está bien
                Toast.makeText(this, "Enlace de recuperación enviado a: $correoIngresado", Toast.LENGTH_LONG).show()

                // Opcional: regresar al login después de 2 segundos
                btnEnviar.postDelayed({ finish() }, 2000)
            }
        }
    }
}