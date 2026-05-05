package com.example.controldemedicamentos

import android.content.Intent
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
import java.sql.ResultSet

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.iniciar_sesion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nsvLoginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los componentes
        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etPassword: TextInputEditText = findViewById(R.id.etPassword)
        val cbKeepSession: CheckBox = findViewById(R.id.cbShowPassword) // Usado para mantener sesión
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnGoogle: Button = findViewById(R.id.btnGoogle)
        val btnCreateAccount: Button = findViewById(R.id.btnCreateAccount)
        val tvForgotPassword: TextView = findViewById(R.id.tvForgotPassword)

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Redirigiendo a recuperación de contraseña...", Toast.LENGTH_SHORT).show()
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnGoogle.setOnClickListener {
            val intent = Intent(this, SeleccionCuentaActivity::class.java)
            startActivity(intent)
        }

        // Botón Iniciar Sesión con Validación Real
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (validarEntradas(email, pass, etEmail, etPassword)) {
                ejecutarLoginEnDB(email, pass, cbKeepSession.isChecked)
            }
        }
    }

    private fun ejecutarLoginEnDB(correo: String, pass: String, mantenerSesion: Boolean) {
        // lifecycleScope evita que la pantalla se congele al conectar a SQL
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos() // Usa tu archivo ConnectSql.kt

            if (connection != null) {
                try {
                    // Consulta para verificar usuario en la tabla que creamos
                    val sql = "SELECT id, nombre, rol FROM Usuarios WHERE correo = ? AND contraseña = ? AND activo = 1"
                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, correo)
                    statement.setString(2, pass)

                    val resultSet: ResultSet = statement.executeQuery()

                    if (resultSet.next()) {
                        val userId = resultSet.getInt("id")
                        val nombre = resultSet.getString("nombre")
                        val rol = resultSet.getString("rol")

                        // Guardar datos en SharedPreferences si el usuario lo desea
                        if (mantenerSesion) {
                            val sharedPref = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putBoolean("isLoggedIn", true)
                                putInt("userId", userId)
                                putString("userName", nombre)
                                putString("userRol", rol)
                                apply()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "¡Bienvenido, $nombre!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, PanelPrincipalActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error de conexión con servidor Baudelio_M_V", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validarEntradas(email: String, pass: String, etEmail: TextInputEditText, etPass: TextInputEditText): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo no válido"
            return false
        }

        if (pass.isEmpty()) {
            etPass.error = "Ingresa tu contraseña"
            return false
        }
        return true
    }
}