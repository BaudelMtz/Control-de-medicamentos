package com.example.controldemedicamentos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
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

// Creamos un pequeño molde para guardar los datos temporalmente
data class MedicamentoSQL(val nombre: String, val forma: String?, val cantidad: Int)

class MedicamentosActivity : AppCompatActivity() {

    private lateinit var contenedorTarjetas: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicamentos)

        contenedorTarjetas = findViewById(R.id.llContenedorTarjetas)

        val btnNuevoMedicamento = findViewById<MaterialButton>(R.id.btnNuevoMedicamento)
        btnNuevoMedicamento.setOnClickListener {
            startActivity(Intent(this, AgregarMedicamentoActivity::class.java))
        }

        // Configurar la flecha de regreso en la barra superior
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Esto simula el botón de "Atrás" del celular
        }
    }

    // Usamos onResume para que, cada vez que regreses a esta pantalla, se vuelva a cargar la lista fresca
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

                    val listaMeds = mutableListOf<MedicamentoSQL>()
                    while (resultSet.next()) {
                        listaMeds.add(
                            MedicamentoSQL(
                                resultSet.getString("nombre"),
                                resultSet.getString("forma_farmaceutica") ?: "",
                                resultSet.getInt("cantidad_disponible")
                            )
                        )
                    }

                    // Ahora dibujamos en la pantalla principal
                    withContext(Dispatchers.Main) {
                        contenedorTarjetas.removeAllViews() // Limpiamos por si había algo antes

                        val inflater = LayoutInflater.from(this@MedicamentosActivity)

                        for (med in listaMeds) {
                            // 1. Clonar la plantilla que creamos en el paso 1
                            val vistaTarjeta = inflater.inflate(R.layout.item_medicamento, contenedorTarjetas, false)

                            // 2. Buscar los elementos dentro de la plantilla
                            val card = vistaTarjeta.findViewById<MaterialCardView>(R.id.cardMedicamento)
                            val icono = vistaTarjeta.findViewById<ImageView>(R.id.ivIconoMed)
                            val tvNombre = vistaTarjeta.findViewById<TextView>(R.id.tvNombreMed)
                            val tvStock = vistaTarjeta.findViewById<TextView>(R.id.tvStockMed)

                            // 3. Ponerle los datos de SQL
                            tvNombre.text = "${med.nombre} (${med.forma})"

                            // Lógica de colores según el stock
                            if (med.cantidad <= 5) {
                                tvStock.text = "Stock crítico: ${med.cantidad} unidades restantes"
                                tvStock.setTextColor(Color.parseColor("#D32F2F")) // Rojo
                                icono.setColorFilter(Color.parseColor("#D32F2F"))
                                card.strokeWidth = 3
                                card.strokeColor = Color.parseColor("#D32F2F")
                                card.setCardBackgroundColor(Color.parseColor("#FFF3F3"))
                            } else {
                                tvStock.text = "En stock: ${med.cantidad} unidades"
                            }

                            // 4. Pegar la tarjeta ya llena en el contenedor
                            contenedorTarjetas.addView(vistaTarjeta)
                        }
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
}