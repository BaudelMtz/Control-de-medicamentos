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
import androidx.lifecycle.lifecycleScope
import conectarBaseDeDatos
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement

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
                // Creamos el objeto usuario con los datos de los campos
                val nuevoUsuario = Usuario(
                    nombre = etName.text.toString(),
                    apellido = etLastName.text.toString(),
                    correo = etEmail.text.toString(),
                    usuario = etEmail.text.toString(), // Usamos correo como usuario por defecto
                    contrasena = etPassword.text.toString(),
                    telefono = etPhone.text.toString(),
                    rol = "Familiar", // Rol por defecto según tu tabla
                    fechaNac = null,
                    googleId = null
                )

                // Ejecutamos el registro en la base de datos
                registrarUsuarioEnDB(nuevoUsuario)
            }
        }

        val btnBackToLogin: TextView = findViewById(R.id.btnRegisterBackToLogin)
        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuarioEnDB(usuario: Usuario) {
        // Usamos corrutinas para no congelar la pantalla mientras se conecta al SQL
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos() // Llama a tu función en ConnectSql.kt

            if (connection != null) {
                try {
                    val sql = """
                        INSERT INTO Usuarios (nombre, apellido, correo, usuario, contraseña, telefono, rol, activo) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                    """.trimIndent()

                    val statement: PreparedStatement = connection.prepareStatement(sql)
                    statement.setString(1, usuario.nombre)
                    statement.setString(2, usuario.apellido)
                    statement.setString(3, usuario.correo)
                    statement.setString(4, usuario.usuario)
                    statement.setString(5, usuario.contrasena)
                    statement.setString(6, usuario.telefono)
                    statement.setString(7, usuario.rol)

                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Usuario registrado en SQL Server", Toast.LENGTH_LONG).show()
                        finish() // Cierra la pantalla y vuelve al Login
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Error al insertar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    connection.close()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show()
                }
            }
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