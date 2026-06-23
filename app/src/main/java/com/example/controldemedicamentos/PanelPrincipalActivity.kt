package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import conectarBaseDeDatos

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerDosis: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_principal)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)

        recyclerDosis = findViewById(R.id.recycler_dosis)
        recyclerDosis.layoutManager = LinearLayoutManager(this)

        cargarDosisDeHoy()

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)

        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio -> { }
                R.id.nav_registrar -> { startActivity(Intent(this, RegisterActivity::class.java)) }
                R.id.nav_medicamentos -> { startActivity(Intent(this, MedicamentosActivity::class.java)) }
                R.id.nav_horarios -> { startActivity(Intent(this, HorariosActivity::class.java)) }
                R.id.nav_reportes -> { startActivity(Intent(this, ReportesActivity::class.java)) }
                R.id.nav_perfiles -> { startActivity(Intent(this, RegistroPacienteActivity::class.java)) }
                R.id.nav_ajustes -> { startActivity(Intent(this, AjustesActivity::class.java)) }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        cargarDosisDeHoy()
    }

    private fun cargarDosisDeHoy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    // ✨ 1. Agregamos D.turno al SELECT para que el Panel sepa de qué turno es cada dosis
                    val sql = """
                        SELECT D.id AS id_dosis, D.id_medicamento, D.hora, D.turno, P.nombre AS paciente, M.nombre AS medicamento 
                        FROM Dosis_Programadas D
                        JOIN Pacientes P ON D.id_paciente = P.id
                        JOIN Medicamentos M ON D.id_medicamento = M.id
                        WHERE D.estado = 'Pendiente' AND D.activa = 1
                        ORDER BY D.hora ASC
                    """.trimIndent()

                    val statement = connection.createStatement()
                    val resultSet = statement.executeQuery(sql)
                    val listaDosisReales = mutableListOf<Dosis>()

                    while (resultSet.next()) {
                        val horaSQL = resultSet.getString("hora")
                        val turnoDosis = resultSet.getString("turno") // Extraemos si es Mañana, Tarde o Noche

                        // Extraemos los números
                        var horaInt = horaSQL.substring(0, 2).toInt()
                        val minutos = horaSQL.substring(3, 5)

                        // Determinamos si es AM o PM
                        var amPm = if (horaInt >= 12) "PM" else "AM"

                        // ✨ 2. Inteligencia para corregir el "03:00" de la tarde/noche en el inicio
                        if ((turnoDosis == "Tarde" || turnoDosis == "Noche") && horaInt < 12) {
                            amPm = "PM"
                        }

                        // Convertimos formato 24h a 12h
                        if (horaInt > 12) horaInt -= 12
                        if (horaInt == 0) horaInt = 12

                        // Armamos el texto en una sola línea para el Panel
                        val horaFormateada = String.format("%02d:%s %s", horaInt, minutos, amPm)

                        listaDosisReales.add(
                            Dosis(
                                resultSet.getInt("id_dosis"),
                                resultSet.getInt("id_medicamento"),
                                horaFormateada,
                                resultSet.getString("paciente"),
                                "Medicamento: ${resultSet.getString("medicamento")}"
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        val adapter = DosisAdapter(listaDosisReales) { dosisSeleccionada ->
                            marcarComoAdministrada(dosisSeleccionada)
                        }
                        recyclerDosis.adapter = adapter
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                } finally {
                    connection.close()
                }
            }
        }
    }

    // ✨ ESTA ES LA FUNCIÓN QUE HACE EL TRABAJO PESADO EN SQL ✨
    private fun marcarComoAdministrada(dosis: Dosis) {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    // 1. Cambiamos la dosis a 'Tomado'
                    val sqlUpdateDosis = "UPDATE Dosis_Programadas SET estado = 'Tomado' WHERE id = ?"
                    val stmt1 = connection.prepareStatement(sqlUpdateDosis)
                    stmt1.setInt(1, dosis.idDosis)
                    stmt1.executeUpdate()

                    // 2. Le restamos 1 al inventario (solo si hay más de 0 para no tener números negativos)
                    val sqlUpdateStock = "UPDATE Medicamentos SET cantidad_disponible = cantidad_disponible - 1 WHERE id = ? AND cantidad_disponible > 0"
                    val stmt2 = connection.prepareStatement(sqlUpdateStock)
                    stmt2.setInt(1, dosis.idMedicamento)
                    stmt2.executeUpdate()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PanelPrincipalActivity, "✅ Dosis registrada. Inventario actualizado.", Toast.LENGTH_SHORT).show()
                        cargarDosisDeHoy() // Recargamos la lista y la tarjeta desaparecerá de pendientes
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