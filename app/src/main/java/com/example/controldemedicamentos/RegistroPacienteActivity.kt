package com.example.controldemedicamentos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import conectarBaseDeDatos

class RegistroPacienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_paciente)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val etNombre = findViewById<TextInputEditText>(R.id.etNombrePaciente)
        val etApellido = findViewById<TextInputEditText>(R.id.etApellidoPaciente)
        val etTelefono = findViewById<TextInputEditText>(R.id.etTelefonoPaciente)
        val etNotas = findViewById<TextInputEditText>(R.id.etNotasPaciente)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardarPaciente)

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val tel = etTelefono.text.toString().trim()
            val notas = etNotas.text.toString().trim()

            if (nombre.isEmpty()) {
                etNombre.error = "El nombre es obligatorio"
                return@setOnClickListener
            }

            guardarPacienteEnSQL(nombre, apellido, tel, notas)
        }
    }

    private fun guardarPacienteEnSQL(nombre: String, apellido: String, tel: String, notas: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    val sql = "INSERT INTO Pacientes (nombre, apellido, telefono, notas_medicas, activo) VALUES (?, ?, ?, ?, 1)"
                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, nombre)
                    statement.setString(2, apellido)
                    statement.setString(3, tel)
                    statement.setString(4, notas)

                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegistroPacienteActivity, "✅ Paciente registrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            }
        }
    }
}