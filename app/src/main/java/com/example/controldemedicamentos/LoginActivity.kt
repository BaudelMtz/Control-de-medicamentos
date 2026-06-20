package com.example.controldemedicamentos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

// --- Nuevas importaciones para Firebase y Google ---
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // 1. Variables globales para Firebase y Google
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.iniciar_sesion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nsvLoginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Inicializamos Firebase Auth
        auth = Firebase.auth

        // 3. Configuramos Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Referencias a los componentes
        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etPassword: TextInputEditText = findViewById(R.id.etPassword)
        val cbKeepSession: CheckBox = findViewById(R.id.cbShowPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnGoogle: Button = findViewById(R.id.btnGoogle)
        val btnCreateAccount: Button = findViewById(R.id.btnCreateAccount)
        val tvForgotPassword: TextView = findViewById(R.id.tvForgotPassword)

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, RecuperarContrasenaActivity::class.java)
            startActivity(intent)
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 4. NUEVO CLICK LISTENER PARA GOOGLE
        btnGoogle.setOnClickListener {
            iniciarSesionConGoogle()
        }

        // Botón Iniciar Sesión con Validación Real (Tu código SQL intacto)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (validarEntradas(email, pass, etEmail, etPassword)) {
                ejecutarLoginEnDB(email, pass, cbKeepSession.isChecked)
            }
        }
    }

    // --- BLOQUE DE FUNCIONES PARA GOOGLE ---
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val cuenta = task.getResult(ApiException::class.java)
                firebaseAuthConGoogle(cuenta.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarSesionConGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthConGoogle(idToken: String) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Bienvenido con Google!", Toast.LENGTH_SHORT).show()
                    // Redirigimos a tu panel principal
                    val intent = Intent(this, PanelPrincipalActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // --- FIN DEL BLOQUE DE GOOGLE ---

    // --- TU CÓDIGO DE SQL SERVER (SIN CAMBIOS) ---
    private fun ejecutarLoginEnDB(correo: String, pass: String, mantenerSesion: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    val sql = "SELECT id, nombre, rol FROM Usuarios WHERE correo = ? AND contraseña = ? AND activo = 1"
                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, correo)
                    statement.setString(2, pass)

                    val resultSet: ResultSet = statement.executeQuery()

                    if (resultSet.next()) {
                        val userId = resultSet.getInt("id")
                        val nombre = resultSet.getString("nombre")
                        val rol = resultSet.getString("rol")

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