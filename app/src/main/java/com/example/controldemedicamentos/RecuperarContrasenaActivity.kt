package com.example.controldemedicamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import conectarBaseDeDatos
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class RecuperarContrasenaActivity : AppCompatActivity() {

    private var captchaVerificado = false

    private val listaPuentesMap = mutableMapOf<Int, Boolean>()
    private val seleccionadasMap = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        val tvVolverInicio = findViewById<TextView>(R.id.tv_volver_inicio)
        val btnEnviar = findViewById<Button>(R.id.btn_enviar)
        val btnVerify = findViewById<Button>(R.id.btn_verify_captcha)
        val etCorreo = findViewById<EditText>(R.id.et_correo)

        val img1 = findViewById<ImageView>(R.id.img_captcha_1)
        val img2 = findViewById<ImageView>(R.id.img_captcha_2)
        val img3 = findViewById<ImageView>(R.id.img_captcha_3)
        val img4 = findViewById<ImageView>(R.id.img_captcha_4)
        val img5 = findViewById<ImageView>(R.id.img_captcha_5)
        val img6 = findViewById<ImageView>(R.id.img_captcha_6)
        val img7 = findViewById<ImageView>(R.id.img_captcha_7)
        val img8 = findViewById<ImageView>(R.id.img_captcha_8)
        val img9 = findViewById<ImageView>(R.id.img_captcha_9)

        inicializarMapaPuentes(img1.id, img2.id, img3.id, img4.id, img5.id, img6.id, img7.id, img8.id, img9.id)

        configurarClicImagen(img1)
        configurarClicImagen(img2)
        configurarClicImagen(img3)
        configurarClicImagen(img4)
        configurarClicImagen(img5)
        configurarClicImagen(img6)
        configurarClicImagen(img7)
        configurarClicImagen(img8)
        configurarClicImagen(img9)

        tvVolverInicio.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnVerify.setOnClickListener {
            if (validarSeleccionCaptcha()) {
                captchaVerificado = true
                Toast.makeText(this, "¡Correcto! Eres un humano.", Toast.LENGTH_SHORT).show()

                btnVerify.text = "VERIFICADO ✔"
                btnVerify.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                btnVerify.isEnabled = false

                deshabilitarClicsImagenes(img1, img2, img3, img4, img5, img6, img7, img8, img9)
            } else {
                Toast.makeText(this, "Selección incorrecta. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                reiniciarCaptcha(img1, img2, img3, img4, img5, img6, img7, img8, img9)
            }
        }

        btnEnviar.setOnClickListener {
            val correoIngresado = etCorreo.text.toString().trim()

            if (correoIngresado.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo primero", Toast.LENGTH_SHORT).show()
            } else if (!captchaVerificado) {
                Toast.makeText(this, "Por favor, completa el CAPTCHA interactivo primero.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Buscando correo en la base de datos...", Toast.LENGTH_SHORT).show()
                btnEnviar.isEnabled = false

                Thread {
                    try {
                        val conexion = conectarBaseDeDatos()

                        if (conexion != null) {
                            val query = "SELECT * FROM Usuarios WHERE correo = ?"
                            val statement = conexion.prepareStatement(query)
                            statement.setString(1, correoIngresado)
                            val resultado = statement.executeQuery()

                            if (resultado.next()) {
                                // Extraemos la contraseña de la base de datos
                                val passwordRecuperada = resultado.getString("contraseña")

                                // Enviamos correo y contraseña
                                enviarCorreoReal(correoIngresado, passwordRecuperada)

                                runOnUiThread {
                                    Toast.makeText(this@RecuperarContrasenaActivity, "Contraseña enviada a: $correoIngresado", Toast.LENGTH_LONG).show()
                                    btnEnviar.postDelayed({ finish() }, 2000)
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@RecuperarContrasenaActivity, "Este correo no está registrado en el sistema", Toast.LENGTH_LONG).show()
                                    btnEnviar.isEnabled = true
                                }
                            }
                            conexion.close()
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@RecuperarContrasenaActivity, "Error: No se pudo conectar al servidor SQL", Toast.LENGTH_LONG).show()
                                btnEnviar.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            Toast.makeText(this@RecuperarContrasenaActivity, "Error al buscar en la base de datos", Toast.LENGTH_SHORT).show()
                            btnEnviar.isEnabled = true
                        }
                    }
                }.start()
            }
        }
    }

    private fun inicializarMapaPuentes(vararg ids: Int) {
        listaPuentesMap[ids[0]] = true
        listaPuentesMap[ids[1]] = false
        listaPuentesMap[ids[2]] = false
        listaPuentesMap[ids[3]] = true
        listaPuentesMap[ids[4]] = false
        listaPuentesMap[ids[5]] = true
        listaPuentesMap[ids[6]] = false
        listaPuentesMap[ids[7]] = false
        listaPuentesMap[ids[8]] = true

        ids.forEach { seleccionadasMap[it] = false }
    }

    private fun configurarClicImagen(imageView: ImageView) {
        imageView.setOnClickListener {
            val id = imageView.id
            val estaSeleccionada = seleccionadasMap[id] ?: false

            if (estaSeleccionada) {
                seleccionadasMap[id] = false
                imageView.alpha = 1.0f
            } else {
                seleccionadasMap[id] = true
                imageView.alpha = 0.5f
            }
        }
    }

    private fun validarSeleccionCaptcha(): Boolean {
        val idsQueSonPuentes = listaPuentesMap.filter { it.value }.keys
        val idsSeleccionadosPorUsuario = seleccionadasMap.filter { it.value }.keys
        return idsQueSonPuentes == idsSeleccionadosPorUsuario
    }

    private fun reiniciarCaptcha(vararg imageViews: ImageView) {
        imageViews.forEach { imageView ->
            seleccionadasMap[imageView.id] = false
            imageView.alpha = 1.0f
        }
    }

    private fun deshabilitarClicsImagenes(vararg imageViews: ImageView) {
        imageViews.forEach { it.isEnabled = false }
    }

    private fun enviarCorreoReal(correoDestino: String, passwordRecuperada: String) {
        val correoRemitente = "bladir.carpio15@gmail.com"
        val passwordAplicacion = "izdnypawiqteucco\n"

        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.socketFactory.port"] = "465"
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "465"

        val session = Session.getDefaultInstance(props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(correoRemitente, passwordAplicacion)
                }
            })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(correoRemitente))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino))
            message.subject = "Recuperación de contraseña - Control de Medicamentos"

            val cuerpoMensaje = "Hola,\n\n" +
                    "Hemos recibido una solicitud para recuperar tu acceso en el sistema de Control de Medicamentos.\n\n" +
                    "Tu contraseña actual es: $passwordRecuperada\n\n" +
                    "Te recomendamos guardarla en un lugar seguro. Si no solicitaste este correo, ignóralo.\n\n" +
                    "Saludos,\nEl equipo de soporte."

            message.setText(cuerpoMensaje)

            Transport.send(message)
            println("Correo enviado exitosamente a $correoDestino")

        } catch (e: MessagingException) {
            e.printStackTrace()
        }
    }
}