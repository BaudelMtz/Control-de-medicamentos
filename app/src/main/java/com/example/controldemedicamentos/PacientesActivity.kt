package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import conectarBaseDeDatos

data class PacienteDirectorio(val id: Int, val nombreCompleto: String, val telefono: String, val notas: String)

class PacientesActivity : AppCompatActivity() {

    private lateinit var contenedorPacientes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pacientes)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        contenedorPacientes = findViewById(R.id.llContenedorPacientes)

        // ✨ 1. Al picar el botón, abrimos tu pantalla de Registro que ya tenías hecha
        val btnNuevoPaciente = findViewById<MaterialButton>(R.id.btnNuevoPaciente)
        btnNuevoPaciente.setOnClickListener {
            val intent = Intent(this, RegistroPacienteActivity::class.java)
            startActivity(intent)
        }
    }

    // Usamos onResume para que al regresar del formulario, la lista se actualice sola
    override fun onResume() {
        super.onResume()
        cargarPacientes()
    }

    private fun cargarPacientes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    // Solo traemos a los pacientes activos (activos = 1)
                    val sql = "SELECT id, nombre, apellido, telefono, notas_medicas FROM Pacientes WHERE activo = 1 ORDER BY nombre ASC"
                    val statement = connection.createStatement()
                    val resultSet = statement.executeQuery(sql)

                    val listaPacientes = mutableListOf<PacienteDirectorio>()

                    while (resultSet.next()) {
                        val nombre = resultSet.getString("nombre") ?: ""
                        val apellido = resultSet.getString("apellido") ?: ""
                        val tel = resultSet.getString("telefono") ?: "Sin teléfono"
                        val notas = resultSet.getString("notas_medicas") ?: "Sin notas"

                        listaPacientes.add(
                            PacienteDirectorio(
                                resultSet.getInt("id"),
                                "$nombre $apellido".trim(),
                                tel,
                                notas
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        contenedorPacientes.removeAllViews()

                        if (listaPacientes.isEmpty()) {
                            return@withContext
                        }

                        val inflater = LayoutInflater.from(this@PacientesActivity)

                        for (paciente in listaPacientes) {
                            val vistaTarjeta = inflater.inflate(R.layout.item_paciente, contenedorPacientes, false)

                            vistaTarjeta.findViewById<TextView>(R.id.tvNombreCompleto).text = paciente.nombreCompleto
                            vistaTarjeta.findViewById<TextView>(R.id.tvTelefonoPaciente).text = "Tel: ${paciente.telefono}"
                            vistaTarjeta.findViewById<TextView>(R.id.tvNotasPaciente).text = "Notas: ${paciente.notas}"

                            // ✨ 2. Lógica para dar de baja con confirmación (Clic Largo)
                            vistaTarjeta.setOnLongClickListener {
                                androidx.appcompat.app.AlertDialog.Builder(this@PacientesActivity)
                                    .setTitle("Dar de baja paciente")
                                    .setMessage("¿Estás seguro de que deseas archivar a ${paciente.nombreCompleto}?\n\nYa no aparecerá en el directorio, pero su historial se conservará.")
                                    .setPositiveButton("Sí, dar de baja") { dialog, _ ->
                                        darDeBajaPaciente(paciente.id)
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                    .show()
                                true
                            }

                            // Nota: Aquí en un futuro puedes agregarle un .setOnClickListener normal para que abra la pantalla de "Editar"

                            contenedorPacientes.addView(vistaTarjeta)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            }
        }
    }

    private fun darDeBajaPaciente(idPaciente: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    // Borrado lógico: cambiamos activo a 0
                    val sql = "UPDATE Pacientes SET activo = 0 WHERE id = ?"
                    val statement = connection.prepareStatement(sql)
                    statement.setInt(1, idPaciente)
                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PacientesActivity, "Paciente archivado", Toast.LENGTH_SHORT).show()
                        cargarPacientes() // Recargamos la lista
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