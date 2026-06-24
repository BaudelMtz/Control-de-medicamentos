package com.example.controldemedicamentos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import conectarBaseDeDatos

data class MedicamentoSQL(val nombre: String, val forma: String?, val cantidad: Int)

class MedicamentosActivity : AppCompatActivity() {

    private lateinit var contenedorTarjetas: LinearLayout
    // ✨ La Lista Maestra que guardará todo en memoria para no saturar el SQL
    private var listaMaestraMeds = mutableListOf<MedicamentoSQL>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicamentos)

        contenedorTarjetas = findViewById(R.id.llContenedorTarjetas)

        val btnNuevoMedicamento = findViewById<MaterialButton>(R.id.btnNuevoMedicamento)
        btnNuevoMedicamento.setOnClickListener {
            startActivity(Intent(this, AgregarMedicamentoActivity::class.java))
        }

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✨ CONFIGURACIÓN DE LA BARRA DE BÚSQUEDA ✨
        val etBuscar = findViewById<EditText>(R.id.etBuscarMedicamento)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Esta función se dispara cada vez que el usuario escribe o borra una letra
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarMedicamentos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        cargarMedicamentosDesdeSQL()
    }

    private fun cargarMedicamentosDesdeSQL() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()

            if (connection != null) {
                try {
                    val sql = "SELECT nombre, forma_farmaceutica, cantidad_disponible FROM Medicamentos WHERE activo = 1"
                    val statement = connection.createStatement()
                    val resultSet = statement.executeQuery(sql)

                    // Limpiamos la lista maestra antes de llenarla con datos frescos
                    listaMaestraMeds.clear()

                    while (resultSet.next()) {
                        listaMaestraMeds.add(
                            MedicamentoSQL(
                                resultSet.getString("nombre"),
                                resultSet.getString("forma_farmaceutica") ?: "",
                                resultSet.getInt("cantidad_disponible")
                            )
                        )
                    }

                    // Dibujamos la lista completa al inicio
                    withContext(Dispatchers.Main) {
                        dibujarTarjetas(listaMaestraMeds)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MedicamentosActivity, "Error conectando al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ✨ FUNCIÓN QUE FILTRA LA LISTA MAESTRA ✨
    private fun filtrarMedicamentos(textoBuscado: String) {
        val textoLimpio = textoBuscado.trim().lowercase()

        // Si la barra está vacía, mostramos todos. Si no, filtramos por nombre.
        val listaFiltrada = if (textoLimpio.isEmpty()) {
            listaMaestraMeds
        } else {
            listaMaestraMeds.filter { med ->
                med.nombre.lowercase().contains(textoLimpio)
            }
        }

        dibujarTarjetas(listaFiltrada)
    }

    // ✨ FUNCIÓN INDEPENDIENTE PARA DIBUJAR TARJETAS ✨
    private fun dibujarTarjetas(listaAMostrar: List<MedicamentoSQL>) {
        contenedorTarjetas.removeAllViews()

        if (listaAMostrar.isEmpty()) {
            // Opcional: Podrías poner un TextView aquí diciendo "No se encontraron resultados"
            return
        }

        val inflater = LayoutInflater.from(this@MedicamentosActivity)

        for (med in listaAMostrar) {
            val vistaTarjeta = inflater.inflate(R.layout.item_medicamento, contenedorTarjetas, false)

            val card = vistaTarjeta.findViewById<MaterialCardView>(R.id.cardMedicamento)
            val icono = vistaTarjeta.findViewById<ImageView>(R.id.ivIconoMed)
            val tvNombre = vistaTarjeta.findViewById<TextView>(R.id.tvNombreMed)
            val tvStock = vistaTarjeta.findViewById<TextView>(R.id.tvStockMed)

            tvNombre.text = "${med.nombre} (${med.forma})"

            if (med.cantidad <= 5) {
                tvStock.text = "Stock crítico: ${med.cantidad} unidades restantes"
                tvStock.setTextColor(Color.parseColor("#D32F2F"))
                icono.setColorFilter(Color.parseColor("#D32F2F"))
                card.strokeWidth = 3
                card.strokeColor = Color.parseColor("#D32F2F")
                card.setCardBackgroundColor(Color.parseColor("#FFF3F3"))
            } else {
                tvStock.text = "En stock: ${med.cantidad} unidades"
            }

            contenedorTarjetas.addView(vistaTarjeta)
        }
    }
}