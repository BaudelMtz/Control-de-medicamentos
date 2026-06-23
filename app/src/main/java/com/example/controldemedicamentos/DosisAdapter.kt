package com.example.controldemedicamentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

// ✨ Agregamos el onAdministradoClick para comunicarnos con el Panel Principal
class DosisAdapter(
    private val listaDosis: List<Dosis>,
    private val onAdministradoClick: (Dosis) -> Unit
) : RecyclerView.Adapter<DosisAdapter.DosisViewHolder>() {

    class DosisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHora: TextView = itemView.findViewById(R.id.tvHoraDosis)
        val tvPaciente: TextView = itemView.findViewById(R.id.tvNombrePaciente)
        val tvMedicamento: TextView = itemView.findViewById(R.id.tvMedicamento)
        val btnAdministrado: MaterialButton = itemView.findViewById(R.id.btnAdministrado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DosisViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dosis, parent, false)
        return DosisViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaDosis.size
    }

    override fun onBindViewHolder(holder: DosisViewHolder, position: Int) {
        val dosisActual = listaDosis[position]

        holder.tvHora.text = dosisActual.hora
        holder.tvPaciente.text = dosisActual.nombrePaciente
        holder.tvMedicamento.text = dosisActual.medicamentoInfo

        // --- LÓGICA DEL BOTÓN ---
        holder.btnAdministrado.setOnClickListener {
            // Le avisamos a la pantalla principal que se presionó este botón y le pasamos los datos
            onAdministradoClick(dosisActual)
        }
    }
}