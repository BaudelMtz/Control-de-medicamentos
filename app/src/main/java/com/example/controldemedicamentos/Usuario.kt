package com.example.controldemedicamentos

data class Usuario(
    val id: Int? = null,
    val nombre: String,
    val apellido: String?,
    val correo: String,
    val usuario: String?,
    val contrasena: String?,
    val telefono: String?,
    val fechaNac: String?,
    val rol: String,
    val googleId: String?,
    val activo: Boolean = true,
    val creadoEn: String? = null,
    val ultimoLogin: String? = null
)