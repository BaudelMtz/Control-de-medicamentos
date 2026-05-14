package com.example.controldemedicamentos

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import conectarBaseDeDatos // Asegúrate de importar tu función de conexión

class AgregarMedicamentoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_medicamento)

        // Referencias a los campos de tu XML (ajusta los IDs según tu diseño)
        val etNombreMed = findViewById<TextInputEditText>(R.id.etNombreMedicamento)
        val etDescripcion = findViewById<TextInputEditText>(R.id.etDescripcion)
        val etCantidad = findViewById<TextInputEditText>(R.id.etCantidad)
        val etForma = findViewById<TextInputEditText>(R.id.etFormaFarmaceutica)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarMedicamento)

        // Configurar la flecha de regreso en la barra superior
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Te regresa a la pantalla anterior
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombreMed.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val cantidadStr = etCantidad.text.toString().trim()
            val forma = etForma.text.toString().trim()

            // Validación básica
            if (nombre.isEmpty() || cantidadStr.isEmpty()) {
                Toast.makeText(this, "El nombre y la cantidad son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cantidad = cantidadStr.toIntOrNull() ?: 0

            guardarMedicamentoEnSQL(nombre, descripcion, cantidad, forma)
        }
    }

    private fun guardarMedicamentoEnSQL(nombre: String, desc: String, cantidad: Int, forma: String) {
        // Mostramos que está cargando
        Toast.makeText(this, "Guardando medicamento...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    val sql = """
                        INSERT INTO Medicamentos (nombre, descripcion, cantidad_disponible, forma_farmaceutica, activo)
                        VALUES (?, ?, ?, ?, 1)
                    """.trimIndent()

                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, nombre)
                    statement.setString(2, desc)
                    statement.setInt(3, cantidad)
                    statement.setString(4, forma)

                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AgregarMedicamentoActivity, "✅ ¡Medicamento guardado con éxito!", Toast.LENGTH_LONG).show()
                        finish() // Cierra esta pantalla y regresa al almacén
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AgregarMedicamentoActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    connection.close()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AgregarMedicamentoActivity, "❌ Error de conexión al servidor", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}