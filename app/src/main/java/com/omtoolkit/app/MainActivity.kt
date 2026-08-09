 package com.omtoolkit.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.zip.ZipInputStream

class MainActivity : Activity() {

lateinit var boton: TextView
lateinit var resultado: TextView

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val pantalla = LinearLayout(this)
    pantalla.orientation = LinearLayout.VERTICAL
    pantalla.gravity = Gravity.CENTER
    pantalla.setPadding(40, 40, 40, 40)

    val titulo = TextView(this)
    titulo.text = "OM Toolkit"
    titulo.textSize = 32f
    titulo.setTypeface(null, Typeface.BOLD)
    titulo.gravity = Gravity.CENTER

    val version = TextView(this)
    version.text = "VERSIÓN 0.3"
    version.textSize = 24f
    version.setTextColor(Color.rgb(0, 140, 70))
    version.gravity = Gravity.CENTER
    version.setPadding(0, 30, 0, 30)

    val texto = TextView(this)
    texto.text = "Analizador de listas Organic Maps"
    texto.textSize = 20f
    texto.gravity = Gravity.CENTER

    boton = TextView(this)
    boton.text = "\n   📂  SELECCIONAR KMZ   \n"
    boton.textSize = 20f
    boton.gravity = Gravity.CENTER
    boton.setPadding(20, 30, 20, 30)

    resultado = TextView(this)
    resultado.text = ""
    resultado.textSize = 18f
    resultado.gravity = Gravity.CENTER
    resultado.setPadding(10, 40, 10, 10)

    pantalla.addView(titulo)
    pantalla.addView(version)
    pantalla.addView(texto)
    pantalla.addView(boton)
    pantalla.addView(resultado)

    boton.setOnClickListener {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_OPEN_DOCUMENT
        )
        intent.type = "application/vnd.google-earth.kmz"
        intent.addCategory(
            android.content.Intent.CATEGORY_OPENABLE
        )
        startActivityForResult(intent, 100)
    }

    setContentView(pantalla)
}

override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: android.content.Intent?
) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 100 && resultCode == RESULT_OK) {

        val archivo = data?.data

        if (archivo != null) {

            try {
                val entrada = contentResolver.openInputStream(archivo)

                if (entrada == null) {
                    resultado.text = "❌ No se pudo abrir el KMZ"
                    return
                }

                val zip = ZipInputStream(entrada)

                var nombreEntrada: String? = null
                var contenidoKml = ""

                var entradaZip = zip.nextEntry

                while (entradaZip != null) {

                    if (entradaZip.name.endsWith(".kml")) {

                        nombreEntrada = entradaZip.name

                        val datos = zip.readBytes()
                        contenidoKml = String(datos, Charsets.UTF_8)

                        break
                    }

                    entradaZip = zip.nextEntry
                }

                zip.close()
                entrada.close()

                if (contenidoKml.isEmpty()) {
                    resultado.text =
                        "❌ No se encontró ningún archivo KML dentro del KMZ"
                    return
                }
                val documentos = Regex(
    "<Document[\\s\\S]*?</Document>",
    RegexOption.IGNORE_CASE
).findAll(contenidoKml)
    .map { it.value }
    .toList()

val listas = documentos
    .map { documento ->
        Regex(
            "<name>(.*?)</name>",
            RegexOption.IGNORE_CASE
        )
            .find(documento)
            ?.groupValues?.get(1)
            ?.trim()
            ?: ""
    }
    .toList()

val listasOrdenadas = documentos
    .zip(listas)
    .sortedBy { it.second.lowercase() })

resultado.text =
    "📁 LISTAS ENCONTRADAS: ${listas.size}\n\n" +
    listasOrdenadas.joinToString("\n") { it.second }

                val marcadores = Regex(
                    "<Point",
                    RegexOption.IGNORE_CASE
                ).findAll(contenidoKml).count()

                val trayectos = Regex(
                    "<gx:Track",
                    RegexOption.IGNORE_CASE
                ).findAll(contenidoKml).count()

                resultado.text =
    "✅ KMZ leído correctamente\n\n" +
    "📄 $nombreEntrada\n\n" +
    "📍 Marcadores: $marcadores\n" +
    "🚶 Trayectos: $trayectos\n\n" +
    "📁 LISTAS ENCONTRADAS: ${listas.size}\n\n" +
    listas.joinToString("\n") +
    "\n\nEl archivo original NO ha sido modificado."

                boton.text = "📂 SELECCIONAR OTRO KMZ"

            } catch (e: Exception) {

                resultado.text =
                    "❌ Error al analizar el KMZ:\n\n${e.message}"
            }
        }
    }
}

}
