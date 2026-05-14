package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_principal)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)

        // --- INICIO DE CONFIGURACIÓN DE LA LISTA (RECYCLERVIEW) ---
        val recyclerDosis: RecyclerView = findViewById(R.id.recycler_dosis)

        // 1. Creamos una lista de prueba idéntica a tu diseño de Balsamiq
        val listaDePrueba = listOf(
            Dosis("10:00 AM", "Nombre del Paciente", "Medicamento: Paracetamol 500mg"),
            Dosis("11:30 AM", "Nombre del Paciente", "Medicamento: Insulina 10 UI"),
            Dosis("1:00 PM", "Nombre del Paciente", "Medicamento: Ibuprofeno 200mg"),
            Dosis("2:45 PM", "Nombre del Paciente", "Medicamento: Losartán 50mg")
        )

        // 2. Le pasamos la lista a nuestro "trabajador" (el Adapter)
        val adapter = DosisAdapter(listaDePrueba)

        // 3. Le decimos a la lista de la pantalla que use este adaptador
        recyclerDosis.adapter = adapter
        // --- FIN DE CONFIGURACIÓN DE LA LISTA ---

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
                R.id.nav_inicio -> {
                    // Ya estamos aquí, solo cerramos el menú
                }
                R.id.nav_registrar -> {
                    startActivity(Intent(this, RegisterActivity::class.java))
                }
                R.id.nav_medicamentos -> {
                    startActivity(Intent(this, MedicamentosActivity::class.java))
                }
                R.id.nav_horarios -> {
                    startActivity(Intent(this, HorariosActivity::class.java))
                }
                R.id.nav_reportes -> {
                    startActivity(Intent(this, ReportesActivity::class.java))
                }
                R.id.nav_perfiles -> {
                    startActivity(Intent(this, RegistroPacienteActivity::class.java))
                }
                R.id.nav_ajustes -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                }
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
}