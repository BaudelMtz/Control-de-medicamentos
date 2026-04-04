package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nsvLoginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los componentes (IDs sincronizados con activity_main.xml)
        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etPassword: TextInputEditText = findViewById(R.id.etPassword)
        val cbKeepSession: CheckBox = findViewById(R.id.cbShowPassword) // Mantenemos el ID actual por ahora
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnGoogle: Button = findViewById(R.id.btnGoogle)
        val btnCreateAccount: Button = findViewById(R.id.btnCreateAccount)

        // Navegación a Registro (Crear cuenta nueva)
        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Navegación a Selección de Cuenta (Google)
        btnGoogle.setOnClickListener {
            val intent = Intent(this, SeleccionCuentaActivity::class.java)
            startActivity(intent)
        }

        // Botón Iniciar Sesión (Simulación de entrada al Panel Principal)
        btnLogin.setOnClickListener {
            val intent = Intent(this, PanelPrincipalActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}