import android.os.StrictMode
import java.sql.Connection
import java.sql.DriverManager

fun conectarBaseDeDatos(): Connection? {
    val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
    StrictMode.setThreadPolicy(policy)
    var conexion: Connection? = null
    val url = "jdbc:jtds:sqlserver://192.168.56.1/PruebasKotlin;user=UsuarioKotlinn;password=12345;"

    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        conexion = DriverManager.getConnection(url)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return conexion
}