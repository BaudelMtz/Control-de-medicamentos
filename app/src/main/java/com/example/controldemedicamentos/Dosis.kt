package com.example.controldemedicamentos

data class Dosis(
    val idDosis: Int,         // ✨ Nuevo: Para saber qué dosis marcar como 'Tomada'
    val idMedicamento: Int,   // ✨ Nuevo: Para saber a qué medicina descontarle stock
    val hora: String,
    val nombrePaciente: String,
    val medicamentoInfo: String
)