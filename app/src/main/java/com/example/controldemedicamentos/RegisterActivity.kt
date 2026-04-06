package com.example.controldemedicamentos

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.crear_cuenta)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etName: TextInputEditText = findViewById(R.id.etName)
        val etLastName: TextInputEditText = findViewById(R.id.etLastName)
        val etPhone: TextInputEditText = findViewById(R.id.etPhone)
        val etPassword: TextInputEditText = findViewById(R.id.etCreatePassword)
        val cbTerms: CheckBox = findViewById(R.id.cbRegisterTerms)
        val cbPrivacy: CheckBox = findViewById(R.id.cbRegisterPrivacy)
        val btnRegister: Button = findViewById(R.id.btnRegisterSubmit)

        btnRegister.setOnClickListener {
            if (validarCampos(etEmail, etName, etLastName, etPhone, etPassword, cbTerms, cbPrivacy)) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
            }
        }

        val btnBackToLogin: TextView = findViewById(R.id.btnRegisterBackToLogin)
        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validarCampos(
        email: TextInputEditText,
        name: TextInputEditText,
        lastName: TextInputEditText,
        phone: TextInputEditText,
        pass: TextInputEditText,
        terms: CheckBox,
        privacy: CheckBox
    ): Boolean {
        val emailText = email.text.toString().trim()
        val nameText = name.text.toString().trim()
        val phoneText = phone.text.toString().trim()
        val passText = pass.text.toString().trim()

        if (emailText.isEmpty()) {
            email.error = "El correo es obligatorio"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            email.error = "Ingresa un correo válido"
            return false
        }

        if (nameText.isEmpty()) {
            name.error = "El nombre es obligatorio"
            return false
        }

        if (phoneText.isEmpty()) {
            phone.error = "El teléfono es obligatorio"
            return false
        } else if (phoneText.length < 8) {
            phone.error = "Mínimo 8 dígitos"
            return false
        }

        if (passText.isEmpty()) {
            pass.error = "La contraseña es obligatoria"
            return false
        } else if (passText.length < 6) {
            pass.error = "Mínimo 6 caracteres"
            return false
        }

        if (!terms.isChecked || !privacy.isChecked) {
            Toast.makeText(this, "Acepta los términos y políticas", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}