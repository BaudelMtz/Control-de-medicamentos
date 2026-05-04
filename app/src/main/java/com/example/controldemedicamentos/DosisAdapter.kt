package com.example.controldemedicamentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

// El adaptador recibe una lista de objetos "Dosis"
class DosisAdapter(private val listaDosis: List<Dosis>) :
    RecyclerView.Adapter<DosisAdapter.DosisViewHolder>() {

    // 1. Esta clase interna "encuentra" los elementos de tu diseño XML (item_dosis.xml)
    class DosisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHora: TextView = itemView.findViewById(R.id.tvHoraDosis)
        val tvPaciente: TextView = itemView.findViewById(R.id.tvNombrePaciente)
        val tvMedicamento: TextView = itemView.findViewById(R.id.tvMedicamento)
        val btnAdministrado: MaterialButton = itemView.findViewById(R.id.btnAdministrado)
    }

    // 2. Aquí "inflamos" (convertimos a código) el diseño XML por cada elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DosisViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dosis, parent, false)
        return DosisViewHolder(view)
    }

    // 3. Le dice al RecyclerView cuántas tarjetas hay en total
    override fun getItemCount(): Int {
        return listaDosis.size
    }

    override fun onBindViewHolder(holder: DosisViewHolder, position: Int) {
        val dosisActual = listaDosis[position]

        holder.tvHora.text = dosisActual.hora
        holder.tvPaciente.text = dosisActual.nombrePaciente
        holder.tvMedicamento.text = dosisActual.medicamentoInfo

        // --- INICIO DE LA LÓGICA DEL BOTÓN ---
        holder.btnAdministrado.setOnClickListener {
            // 1.Mostramos un mensaje flotante
            android.widget.Toast.makeText(
                holder.itemView.context,
                "Dosis de ${dosisActual.nombrePaciente} registrada",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // 2. Cambiamos la apariencia del botón para simular que ya se administró
            holder.btnAdministrado.text = "Administrado"
            holder.btnAdministrado.setBackgroundColor(android.graphics.Color.parseColor("#95A5A6")) // Gris
            holder.btnAdministrado.isEnabled = false // Lo deshabilitamos para que no le den doble clic

            // NOTA PARA DESPUES: Aquí es donde pondremos el código de Retrofit
            // para avisarle a SQL Server que esta medicina ya se dio.
        }
    }
}