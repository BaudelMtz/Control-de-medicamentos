package com.example.controldemedicamentos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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

class AsignarDosisActivity : AppCompatActivity() {

    // Diccionarios para relacionar el nombre que ve el usuario con el ID de SQL
    private val mapaPacientes = mutableMapOf<String, Int>()
    private val mapaMedicamentos = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_dosis)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Configurar el menú de Turnos (Esto es fijo, no viene de SQL)
        val turnos = arrayOf("Mañana", "Tarde", "Noche")
        val adapterTurno = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, turnos)
        val actvTurno = findViewById<AutoCompleteTextView>(R.id.actvTurno)
        actvTurno.setAdapter(adapterTurno)

        // Cargar Pacientes y Medicamentos de SQL
        cargarListasDesdeSQL()

        val btnAsignar = findViewById<MaterialButton>(R.id.btnAsignarDosis)
        btnAsignar.setOnClickListener {
            guardarDosis()
        }
    }

    private fun cargarListasDesdeSQL() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    // 1. Cargar Pacientes
                    val rsPacientes = connection.createStatement().executeQuery("SELECT id, nombre, apellido FROM Pacientes WHERE activo = 1")
                    val listaNombresPacientes = mutableListOf<String>()
                    while (rsPacientes.next()) {
                        val id = rsPacientes.getInt("id")
                        val nombreCompleto = "${rsPacientes.getString("nombre")} ${rsPacientes.getString("apellido") ?: ""}".trim()
                        mapaPacientes[nombreCompleto] = id
                        listaNombresPacientes.add(nombreCompleto)
                    }

                    // 2. Cargar Medicamentos
                    val rsMedicamentos = connection.createStatement().executeQuery("SELECT id, nombre FROM Medicamentos WHERE activo = 1")
                    val listaNombresMeds = mutableListOf<String>()
                    while (rsMedicamentos.next()) {
                        val id = rsMedicamentos.getInt("id")
                        val nombre = rsMedicamentos.getString("nombre")
                        mapaMedicamentos[nombre] = id
                        listaNombresMeds.add(nombre)
                    }

                    // Actualizar la pantalla (UI)
                    withContext(Dispatchers.Main) {
                        val actvPaciente = findViewById<AutoCompleteTextView>(R.id.actvPaciente)
                        val adapterPacientes = ArrayAdapter(this@AsignarDosisActivity, android.R.layout.simple_dropdown_item_1line, listaNombresPacientes)
                        actvPaciente.setAdapter(adapterPacientes)

                        val actvMedicamento = findViewById<AutoCompleteTextView>(R.id.actvMedicamento)
                        val adapterMeds = ArrayAdapter(this@AsignarDosisActivity, android.R.layout.simple_dropdown_item_1line, listaNombresMeds)
                        actvMedicamento.setAdapter(adapterMeds)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            }
        }
    }

    private fun guardarDosis() {
        val pacienteSeleccionado = findViewById<AutoCompleteTextView>(R.id.actvPaciente).text.toString()
        val medicamentoSeleccionado = findViewById<AutoCompleteTextView>(R.id.actvMedicamento).text.toString()
        val hora = findViewById<TextInputEditText>(R.id.etHora).text.toString().trim()
        val turno = findViewById<AutoCompleteTextView>(R.id.actvTurno).text.toString()
        val indicaciones = findViewById<TextInputEditText>(R.id.etIndicaciones).text.toString().trim()

        // Validaciones básicas
        if (pacienteSeleccionado.isEmpty() || medicamentoSeleccionado.isEmpty() || hora.isEmpty() || turno.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener los IDs reales
        val idPaciente = mapaPacientes[pacienteSeleccionado]
        val idMedicamento = mapaMedicamentos[medicamentoSeleccionado]

        if (idPaciente == null || idMedicamento == null) {
            Toast.makeText(this, "Error seleccionando paciente o medicamento", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar en SQL
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    val sql = "INSERT INTO Dosis_Programadas (id_paciente, id_medicamento, hora, turno, indicaciones, estado) VALUES (?, ?, ?, ?, ?, 'Pendiente')"
                    val statement = connection.prepareStatement(sql)
                    // Como el tipo en SQL es TIME, concatenamos ":00" para los segundos si el usuario escribió "08:30"
                    val horaSql = if (hora.length == 5) "$hora:00" else hora

                    statement.setInt(1, idPaciente)
                    statement.setInt(2, idMedicamento)
                    statement.setString(3, horaSql)
                    statement.setString(4, turno)
                    statement.setString(5, indicaciones)

                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AsignarDosisActivity, "✅ Horario guardado correctamente", Toast.LENGTH_SHORT).show()
                        finish() // Cierra la pantalla y vuelve a la anterior
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