package com.example.controldemedicamentos

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

// Creamos un molde para guardar los datos de SQL temporalmente
data class DosisHorario(val paciente: String, val medicamento: String, val hora: String)

class HorariosActivity : AppCompatActivity() {

    // Contenedor donde se inyectarán las tarjetas (Asegúrate de tener un LinearLayout vacío con este ID en tu XML)
    private lateinit var contenedorHorarios: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        // Configuración de la barra superior (Toolbar)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Buscar el contenedor de las tarjetas
        // NOTA: Debes asegurarte de que en tu activity_horarios.xml tienes un LinearLayout con el id: llContenedorHorarios
        contenedorHorarios = findViewById(R.id.llContenedorHorarios)

        // Buscar los 3 botones de arriba
        // NOTA: Ajusta estos IDs si les pusiste otros nombres en tu XML
        val btnManana = findViewById<MaterialButton>(R.id.btnManana)
        val btnTarde = findViewById<MaterialButton>(R.id.btnTarde)
        val btnNoche = findViewById<MaterialButton>(R.id.btnNoche)

        // Al hacer clic, le pasamos la palabra clave a SQL
        btnManana.setOnClickListener { cargarDosisPorTurno("Mañana") }
        btnTarde.setOnClickListener { cargarDosisPorTurno("Tarde") }
        btnNoche.setOnClickListener { cargarDosisPorTurno("Noche") }

        // Cargar el turno de la Mañana automáticamente al abrir la pantalla
        cargarDosisPorTurno("Mañana")
    }

    private fun cargarDosisPorTurno(turnoFiltro: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    // El Query con los JOIN para traer nombres reales en vez de números de ID
                    val sql = """
                        SELECT P.nombre AS paciente, M.nombre AS medicamento, D.hora 
                        FROM Dosis_Programadas D
                        JOIN Pacientes P ON D.id_paciente = P.id
                        JOIN Medicamentos M ON D.id_medicamento = M.id
                        WHERE D.turno = ? AND D.estado = 'Pendiente'
                    """.trimIndent()

                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, turnoFiltro)

                    val resultSet = statement.executeQuery()
                    val listaDosis = mutableListOf<DosisHorario>()

                    while (resultSet.next()) {
                        // SQL devuelve la hora completa "08:00:00", la cortamos a "08:00"
                        var horaCompleta = resultSet.getString("hora")
                        if (horaCompleta.length >= 5) {
                            horaCompleta = horaCompleta.substring(0, 5)
                        }

                        listaDosis.add(
                            DosisHorario(
                                resultSet.getString("paciente"),
                                resultSet.getString("medicamento"),
                                horaCompleta
                            )
                        )
                    }

                    // Pasar los datos a la interfaz visual
                    withContext(Dispatchers.Main) {
                        contenedorHorarios.removeAllViews() // Limpiar pantalla al cambiar de pestaña

                        if (listaDosis.isEmpty()) {
                            Toast.makeText(this@HorariosActivity, "No hay pendientes para la $turnoFiltro", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        val inflater = LayoutInflater.from(this@HorariosActivity)

                        for (dosis in listaDosis) {
                            // 1. Clonar el diseño de la tarjeta (el archivo item_horario.xml)
                            val vistaTarjeta = inflater.inflate(R.layout.item_horario, contenedorHorarios, false)

                            // 2. Buscar las cajas de texto en ese diseño
                            val tvHora = vistaTarjeta.findViewById<TextView>(R.id.tvHoraDosis)
                            val tvPaciente = vistaTarjeta.findViewById<TextView>(R.id.tvNombrePacienteDosis)
                            val tvMedicamento = vistaTarjeta.findViewById<TextView>(R.id.tvMedicamentoDosis)

                            // 3. Pegar los datos de SQL
                            tvHora.text = "${dosis.hora}\nAM" // Puedes mejorar la lógica AM/PM después
                            tvPaciente.text = dosis.paciente
                            tvMedicamento.text = dosis.medicamento

                            // 4. Inyectar en la pantalla
                            contenedorHorarios.addView(vistaTarjeta)
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
}