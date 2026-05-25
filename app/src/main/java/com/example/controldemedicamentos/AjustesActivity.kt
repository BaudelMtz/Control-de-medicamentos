package com.example.controldemedicamentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AjustesActivity : AppCompatActivity() {

    // Variable para Firebase que añadimos nosotros
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        // ==========================================
        // CÓDIGO DE TU COMPAÑERO (Barra de navegación) - INTACTO
        // ==========================================
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ==========================================
        // TU CÓDIGO (Botón de Cerrar Sesión)
        // ==========================================
        // Inicializamos Firebase Auth
        auth = Firebase.auth

        // Enlazamos el botón rojo de la interfaz (asegúrate de que el ID coincida con el XML)
        val btnCerrarSesion: Button = findViewById(R.id.btnCerrarSesion)

        btnCerrarSesion.setOnClickListener {
            cerrarSesionCompleta()
        }
    }

    // ==========================================
    // FUNCIÓN PARA DESCONECTAR A GOOGLE Y FIREBASE
    // ==========================================
    private fun cerrarSesionCompleta() {
        // 1. Cerramos la sesión de Firebase
        auth.signOut()

        // 2. Configuramos el cliente de Google para pedirle que olvide la cuenta
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Cerramos la sesión de Google (para que vuelva a salir la ventana de cuentas)
        googleSignInClient.signOut().addOnCompleteListener(this) {

            // 4. Borramos las preferencias guardadas (tu sesión de base de datos)
            val sharedPref = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            // 5. Redirigimos al usuario de vuelta a la pantalla de Login
            val intent = Intent(this, LoginActivity::class.java)
            // Estas banderas evitan que el usuario regrese con el botón "Atrás" del celular
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}