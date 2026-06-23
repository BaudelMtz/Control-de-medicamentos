package com.example.controldemedicamentos

import android.content.Intent
import android.content.res.ColorStateList
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

data class DosisHorario(val id: Int, val paciente: String, val medicamento: String, val hora: String)

class HorariosActivity : AppCompatActivity() {

    private lateinit var contenedorHorarios: LinearLayout
    // ✨ 1. Guardamos cuál es la pestaña actual para saber qué recargar al volver
    private var turnoActual = "Mañana"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        contenedorHorarios = findViewById(R.id.llContenedorHorarios)

        val btnManana = findViewById<MaterialButton>(R.id.btnManana)
        val btnTarde = findViewById<MaterialButton>(R.id.btnTarde)
        val btnNoche = findViewById<MaterialButton>(R.id.btnNoche)

        btnManana.setOnClickListener {
            turnoActual = "Mañana" // Actualizamos la memoria
            activarBotonPestana(btnManana, btnTarde, btnNoche)
            cargarDosisPorTurno(turnoActual)
        }
        btnTarde.setOnClickListener {
            turnoActual = "Tarde"
            activarBotonPestana(btnTarde, btnManana, btnNoche)
            cargarDosisPorTurno(turnoActual)
        }
        btnNoche.setOnClickListener {
            turnoActual = "Noche"
            activarBotonPestana(btnNoche, btnManana, btnTarde)
            cargarDosisPorTurno(turnoActual)
        }

        val btnNuevaDosis = findViewById<MaterialButton>(R.id.btnNuevaDosis)
        btnNuevaDosis.setOnClickListener {
            val intent = Intent(this, AsignarDosisActivity::class.java)
            startActivity(intent)
        }

        // Estado inicial
        activarBotonPestana(btnManana, btnTarde, btnNoche)
        cargarDosisPorTurno(turnoActual)
    }

    // ✨ 2. Esta función de Android se ejecuta SIEMPRE que regresas a esta pantalla
    override fun onResume() {
        super.onResume()
        // Recargamos silenciosamente la pestaña en la que estábamos
        cargarDosisPorTurno(turnoActual)
    }

    private fun activarBotonPestana(activo: MaterialButton, inactivo1: MaterialButton, inactivo2: MaterialButton) {
        val colorVerde = resources.getColor(R.color.primary_green, theme)
        val colorBlanco = resources.getColor(R.color.white, theme)
        val colorTransparente = resources.getColor(android.R.color.transparent, theme)

        activo.backgroundTintList = ColorStateList.valueOf(colorVerde)
        activo.setTextColor(colorBlanco)

        inactivo1.backgroundTintList = ColorStateList.valueOf(colorTransparente)
        inactivo1.setTextColor(colorVerde)

        inactivo2.backgroundTintList = ColorStateList.valueOf(colorTransparente)
        inactivo2.setTextColor(colorVerde)
    }

    private fun cargarDosisPorTurno(turnoFiltro: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    val sql = """
                        SELECT D.id, P.nombre AS paciente, M.nombre AS medicamento, D.hora 
                        FROM Dosis_Programadas D
                        JOIN Pacientes P ON D.id_paciente = P.id
                        JOIN Medicamentos M ON D.id_medicamento = M.id
                        WHERE D.turno = ? AND D.estado = 'Pendiente' AND D.activa = 1
                        ORDER BY D.hora ASC
                    """.trimIndent()

                    val statement = connection.prepareStatement(sql)
                    statement.setString(1, turnoFiltro)

                    val resultSet = statement.executeQuery()
                    val listaDosis = mutableListOf<DosisHorario>()

                    while (resultSet.next()) {
                        val horaSQL = resultSet.getString("hora")
                        var horaInt = horaSQL.substring(0, 2).toInt()
                        val minutos = horaSQL.substring(3, 5)

                        var amPm = if (horaInt >= 12) "PM" else "AM"

                        // ✨ 3. Inteligencia para corregir el "03:00" de la tarde/noche
                        if ((turnoFiltro == "Tarde" || turnoFiltro == "Noche") && horaInt < 12) {
                            amPm = "PM"
                        }

                        if (horaInt > 12) horaInt -= 12
                        if (horaInt == 0) horaInt = 12

                        val horaFormateada = String.format("%02d:%s\n%s", horaInt, minutos, amPm)

                        listaDosis.add(
                            DosisHorario(
                                resultSet.getInt("id"),
                                resultSet.getString("paciente"),
                                resultSet.getString("medicamento"),
                                horaFormateada
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        contenedorHorarios.removeAllViews()

                        if (listaDosis.isEmpty()) {
                            // Pequeño cambio: si está vacío, no mandamos un Toast para que no sea molesto al regresar,
                            // solo dejamos la pantalla vacía.
                            return@withContext
                        }

                        val inflater = LayoutInflater.from(this@HorariosActivity)

                        for (dosis in listaDosis) {
                            val vistaTarjeta = inflater.inflate(R.layout.item_horario, contenedorHorarios, false)

                            val tvHora = vistaTarjeta.findViewById<TextView>(R.id.tvHoraDosis)
                            val tvPaciente = vistaTarjeta.findViewById<TextView>(R.id.tvNombrePacienteDosis)
                            val tvMedicamento = vistaTarjeta.findViewById<TextView>(R.id.tvMedicamentoDosis)

                            tvHora.text = dosis.hora
                            tvPaciente.text = dosis.paciente
                            tvMedicamento.text = dosis.medicamento

                            vistaTarjeta.setOnLongClickListener {
                                androidx.appcompat.app.AlertDialog.Builder(this@HorariosActivity)
                                    .setTitle("Suspender Tratamiento")
                                    .setMessage("¿Estás seguro de que deseas suspender la dosis de ${dosis.medicamento} para ${dosis.paciente}?")
                                    .setPositiveButton("Sí, suspender") { dialog, _ ->
                                        suspenderDosisEnSQL(dosis.id, turnoFiltro)
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("Cancelar") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()
                                true
                            }

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

    private fun suspenderDosisEnSQL(idDosis: Int, turnoActual: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    val sql = "UPDATE Dosis_Programadas SET activa = 0 WHERE id = ?"
                    val statement = connection.prepareStatement(sql)
                    statement.setInt(1, idDosis)
                    statement.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HorariosActivity, "Dosis suspendida correctamente", Toast.LENGTH_SHORT).show()
                        cargarDosisPorTurno(turnoActual)
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