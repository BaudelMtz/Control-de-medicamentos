package com.example.controldemedicamentos

import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.util.Properties
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogDosis(val paciente: String, val medicamento: String, val horaFormateada: String)
data class AlertaStock(val medicamento: String, val cantidad: Int)

class ReportesActivity : AppCompatActivity() {

    private lateinit var contenedorActividad: LinearLayout

    // ✨ VARIABLES GLOBALES PARA GUARDAR LOS DATOS PARA EL PDF ✨
    private var totalDosisHoy = 0
    private var totalPendientes = 0
    private val listaAlertasGlobal = mutableListOf<AlertaStock>()
    private val listaHistorialGlobal = mutableListOf<LogDosis>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white, theme))

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        contenedorActividad = findViewById(R.id.llContenedorActividad)

        // ✨ CONECTAMOS EL BOTÓN DEL PDF ✨
        val btnDescargarPdf = findViewById<MaterialButton>(R.id.btnDescargarPdf)
        btnDescargarPdf.setOnClickListener {
            generarPDF()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEstadisticasReporte()
        cargarActividadReciente()
    }

    private fun cargarEstadisticasReporte() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    val stmt = connection.createStatement()

                    val rs1 = stmt.executeQuery("SELECT COUNT(*) FROM Dosis_Programadas WHERE activa = 1")
                    totalDosisHoy = if (rs1.next()) rs1.getInt(1) else 0

                    val rs2 = stmt.executeQuery("SELECT COUNT(*) FROM Dosis_Programadas WHERE activa = 1 AND estado = 'Pendiente'")
                    totalPendientes = if (rs2.next()) rs2.getInt(1) else 0

                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.tvReporteDosisHoy)?.text = totalDosisHoy.toString()
                        findViewById<TextView>(R.id.tvReportePendientes)?.text = totalPendientes.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection.close()
                }
            }
        }
    }

    private fun cargarActividadReciente() {
        lifecycleScope.launch(Dispatchers.IO) {
            val connection = conectarBaseDeDatos()
            if (connection != null) {
                try {
                    val stmt = connection.createStatement()

                    // Alertas
                    val sqlAlertas = "SELECT nombre, cantidad_disponible FROM Medicamentos WHERE activo = 1 AND cantidad_disponible <= 5 ORDER BY cantidad_disponible ASC"
                    val rsAlertas = stmt.executeQuery(sqlAlertas)

                    listaAlertasGlobal.clear()
                    while (rsAlertas.next()) {
                        listaAlertasGlobal.add(AlertaStock(rsAlertas.getString("nombre"), rsAlertas.getInt("cantidad_disponible")))
                    }

                    // Historial
                    val sqlHistorial = """
                        SELECT P.nombre AS paciente, M.nombre AS medicamento, D.hora, D.turno
                        FROM Dosis_Programadas D
                        JOIN Pacientes P ON D.id_paciente = P.id
                        JOIN Medicamentos M ON D.id_medicamento = M.id
                        WHERE D.estado = 'Tomado' AND D.activa = 1
                        ORDER BY D.hora DESC
                    """.trimIndent()

                    val rsHistorial = stmt.executeQuery(sqlHistorial)

                    listaHistorialGlobal.clear()
                    while (rsHistorial.next()) {
                        val horaSQL = rsHistorial.getString("hora")
                        val turno = rsHistorial.getString("turno")

                        var horaInt = horaSQL.substring(0, 2).toInt()
                        val minutos = horaSQL.substring(3, 5)
                        var amPm = if (horaInt >= 12) "PM" else "AM"
                        if ((turno == "Tarde" || turno == "Noche") && horaInt < 12) amPm = "PM"
                        if (horaInt > 12) horaInt -= 12
                        if (horaInt == 0) horaInt = 12
                        val horaFormateada = String.format("%02d:%s %s", horaInt, minutos, amPm)

                        listaHistorialGlobal.add(LogDosis(rsHistorial.getString("paciente"), rsHistorial.getString("medicamento"), horaFormateada))
                    }

                    // DIBUJAR PANTALLA
                    withContext(Dispatchers.Main) {
                        contenedorActividad.removeAllViews()

                        val inflater = LayoutInflater.from(this@ReportesActivity)
                        val totalElementos = listaAlertasGlobal.size + listaHistorialGlobal.size

                        if (totalElementos == 0) {
                            val vistaNoActividad = inflater.inflate(R.layout.item_actividad, contenedorActividad, false)
                            vistaNoActividad.findViewById<TextView>(R.id.tvTextoActividad).text = "Sin actividad ni alertas registradas hoy."
                            vistaNoActividad.findViewById<TextView>(R.id.tvSubtextoActividad).visibility = View.GONE
                            vistaNoActividad.findViewById<View>(R.id.lineaDivisoria).visibility = View.GONE
                            contenedorActividad.addView(vistaNoActividad)
                            return@withContext
                        }

                        // Pintar Alertas
                        for (alerta in listaAlertasGlobal) {
                            val vistaFila = inflater.inflate(R.layout.item_actividad, contenedorActividad, false)
                            val tvTexto = vistaFila.findViewById<TextView>(R.id.tvTextoActividad)
                            val tvSubtexto = vistaFila.findViewById<TextView>(R.id.tvSubtextoActividad)

                            tvTexto.text = "• Alerta: Stock bajo de ${alerta.medicamento}"
                            tvTexto.setTextColor(Color.parseColor("#D32F2F"))
                            tvSubtexto.text = "Quedan únicamente ${alerta.cantidad} unidades en farmacia."

                            contenedorActividad.addView(vistaFila)
                        }

                        // Pintar Historial
                        for (log in listaHistorialGlobal) {
                            val vistaFila = inflater.inflate(R.layout.item_actividad, contenedorActividad, false)
                            val tvTexto = vistaFila.findViewById<TextView>(R.id.tvTextoActividad)
                            val tvSubtexto = vistaFila.findViewById<TextView>(R.id.tvSubtextoActividad)

                            tvTexto.text = "• ${log.paciente} recibió ${log.medicamento}"
                            tvTexto.setTextColor(Color.BLACK)
                            tvSubtexto.text = "Programado a las ${log.horaFormateada}"

                            contenedorActividad.addView(vistaFila)
                        }

                        if (contenedorActividad.childCount > 0) {
                            val ultimaFila = contenedorActividad.getChildAt(contenedorActividad.childCount - 1)
                            ultimaFila.findViewById<View>(R.id.lineaDivisoria).visibility = View.GONE
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

    // ✨ FASE 3: LÓGICA PARA CREAR EL ARCHIVO PDF ✨
    private fun generarPDF() {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val tituloPaint = Paint()

        // Creamos una hoja tamaño carta estándar
        val pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Dibujar el Título
        tituloPaint.textAlign = Paint.Align.CENTER
        tituloPaint.textSize = 16f
        tituloPaint.isFakeBoldText = true
        canvas.drawText("Reporte General - Impulso de Vida", 200f, 40f, tituloPaint)

        // 2. Dibujar la Fecha
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        val fechaActual = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generado el: $fechaActual", 20f, 70f, paint)

        // 3. Dibujar Estadísticas
        paint.color = Color.BLACK
        paint.textSize = 12f
        canvas.drawText("Dosis Programadas Hoy: $totalDosisHoy", 20f, 100f, paint)
        canvas.drawText("Dosis Pendientes: $totalPendientes", 20f, 120f, paint)

        var posicionY = 160f // Llevamos el control de la altura para no encimar textos

        // 4. Dibujar Alertas
        tituloPaint.textSize = 12f
        tituloPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ALERTAS DE STOCK:", 20f, posicionY, tituloPaint)
        posicionY += 20f

        paint.color = Color.RED
        if (listaAlertasGlobal.isEmpty()) {
            paint.color = Color.BLACK
            canvas.drawText("Todo en orden. No hay alertas de stock.", 20f, posicionY, paint)
            posicionY += 20f
        } else {
            for (alerta in listaAlertasGlobal) {
                canvas.drawText(
                    "- ${alerta.medicamento} (Solo quedan ${alerta.cantidad})",
                    20f,
                    posicionY,
                    paint
                )
                posicionY += 20f
            }
        }

        // 5. Dibujar Historial
        paint.color = Color.BLACK
        posicionY += 10f
        canvas.drawText("HISTORIAL DE ADMINISTRACIÓN:", 20f, posicionY, tituloPaint)
        posicionY += 20f

        if (listaHistorialGlobal.isEmpty()) {
            canvas.drawText(
                "No se han registrado dosis administradas el día de hoy.",
                20f,
                posicionY,
                paint
            )
        } else {
            for (log in listaHistorialGlobal) {
                // Evitamos que se salga de la página virtual
                if (posicionY > 550f) {
                    canvas.drawText("... (Historial truncado por espacio)", 20f, posicionY, paint)
                    break
                }
                canvas.drawText(
                    "- ${log.paciente} recibió ${log.medicamento} a las ${log.horaFormateada}",
                    20f,
                    posicionY,
                    paint
                )
                posicionY += 20f
            }
        }

        pdfDocument.finishPage(page)

        // 6. Guardar en los Documentos internos de la App (Para evitar problemas de permisos)
// Pon esta en su lugar:
        // Lo guardamos de forma privada temporalmente solo para poder enviarlo
        val file =
            File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reporte_ImpulsoDeVida.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))

            // Avisamos al usuario que el proceso comenzó (enviar correos toma un par de segundos)
            Toast.makeText(this, "Generando PDF y enviando al correo...", Toast.LENGTH_SHORT).show()

            // Usamos un Thread para no congelar la pantalla mientras se manda el correo
            Thread {
                try {
                    // ✨ 1. LEEMOS EL CORREO DEL "GAFETE" (SharedPreferences) ✨
                    val sharedPreferences = getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                    val correoEnfermero = sharedPreferences.getString("correo_usuario", "") ?: ""

                    if (correoEnfermero.isNotEmpty()) {
                        enviarReportePorCorreo(file, correoEnfermero)

                        runOnUiThread {
                            Toast.makeText(this@ReportesActivity, "¡Reporte enviado a $correoEnfermero!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ReportesActivity, "Error: No se encontró el correo de la sesión actual.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ReportesActivity, "Error al enviar el correo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun enviarReportePorCorreo(archivoPdf: File, correoDestino: String) {
        val correoRemitente = "bladir.carpio15@gmail.com"
        // Le quité el "\n" al final de la contraseña para evitar errores ocultos
        val passwordAplicacion = "izdnypawiqteucco"

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
            message.subject = "Reporte General Diario - Impulso de Vida"

            // 1. Creamos la parte del texto (El cuerpo del correo)
            val parteTexto = MimeBodyPart()
            parteTexto.setText("Hola,\n\nAdjunto encontrarás el reporte general generado desde el sistema Impulso de Vida con las estadísticas de hoy y el inventario crítico.\n\nSaludos,\nEl equipo de soporte.")

            // 2. Creamos la parte del archivo (El PDF adjunto)
            val parteArchivo = MimeBodyPart()
            val fuenteArchivo = FileDataSource(archivoPdf)
            parteArchivo.dataHandler = DataHandler(fuenteArchivo)
            parteArchivo.fileName = archivoPdf.name

            // 3. Juntamos el texto y el archivo en un "Multipart"
            val correoCompleto = MimeMultipart()
            correoCompleto.addBodyPart(parteTexto)
            correoCompleto.addBodyPart(parteArchivo)

            // 4. Metemos todo al mensaje y lo enviamos
            message.setContent(correoCompleto)
            Transport.send(message)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}