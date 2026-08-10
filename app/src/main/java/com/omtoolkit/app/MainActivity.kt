package com.omtoolkit.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class MainActivity : Activity() {

    lateinit var boton: TextView
    lateinit var resultado: TextView
    lateinit var listaContenedor: LinearLayout

    data class ListaKml(
        val nombre: String,
        val documento: String,
        val marcadores: Int,
        val trayectos: Int
    )

    var listasEncontradas = mutableListOf<ListaKml>()

    var listaParaGuardar = -1

    var nombreKmlOriginal = "OrganicMaps.kml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)

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
        version.text = "VERSIÓN 0.6"
        version.textSize = 24f
        version.setTextColor(Color.rgb(0, 140, 70))
        version.gravity = Gravity.CENTER
        version.setPadding(0, 30, 0, 30)

        val texto = TextView(this)
        texto.text = "Separador y ordenador de listas Organic Maps"
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
        resultado.setPadding(10, 30, 10, 10)

        listaContenedor = LinearLayout(this)
        listaContenedor.orientation = LinearLayout.VERTICAL
        listaContenedor.setPadding(0, 20, 0, 20)

        pantalla.addView(titulo)
        pantalla.addView(version)
        pantalla.addView(texto)
        pantalla.addView(boton)
        pantalla.addView(resultado)
        pantalla.addView(listaContenedor)

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

        scroll.addView(pantalla)

        setContentView(scroll)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        // ============================================================
        // ABRIR KMZ
        // ============================================================

        if (
            requestCode == 100 &&
            resultCode == RESULT_OK
        ) {

            val archivo = data?.data

            if (archivo == null) {
                return
            }

            try {

                val entrada =
                    contentResolver.openInputStream(archivo)

                if (entrada == null) {

                    resultado.text =
                        "❌ No se pudo abrir el KMZ"

                    return
                }

                val bytes =
                    entrada.readBytes()

                entrada.close()

                val zip =
                    ZipInputStream(
                        ByteArrayInputStream(bytes)
                    )

                var contenidoKml = ""
                var nombreKml = "OrganicMaps.kml"

                var entradaZip =
                    zip.nextEntry

                while (entradaZip != null) {

                    if (
                        entradaZip.name
                            .lowercase()
                            .endsWith(".kml")
                    ) {

                        nombreKml =
                            entradaZip.name

                        val datos =
                            zip.readBytes()

                        contenidoKml =
                            String(
                                datos,
                                Charsets.UTF_8
                            )

                        break
                    }

                    entradaZip =
                        zip.nextEntry
                }

                zip.close()

                if (contenidoKml.isEmpty()) {

                    resultado.text =
                        "❌ No se encontró ningún KML dentro del KMZ"

                    return
                }

                nombreKmlOriginal =
                    nombreKml

                // ====================================================
                // BUSCAR DOCUMENTOS COMPLETOS
                // ====================================================

                val documentos =
                    Regex(
                        "<Document\\b[\\s\\S]*?</Document>",
                        RegexOption.IGNORE_CASE
                    )
                        .findAll(contenidoKml)
                        .map {
                            it.value
                        }
                        .toList()

                if (documentos.isEmpty()) {

                    resultado.text =
                        "❌ No se encontraron listas en el KML"

                    return
                }

                listasEncontradas.clear()

                // ====================================================
                // ANALIZAR CADA LISTA
                // ====================================================

                for (documento in documentos) {

                    val nombre =
                        Regex(
                            "<name>([\\s\\S]*?)</name>",
                            RegexOption.IGNORE_CASE
                        )
                            .find(documento)
                            ?.groupValues
                            ?.get(1)
                            ?.trim()
                            ?: "Lista sin nombre"

                    val marcadores =
                        Regex(
                            "<Point\\b",
                            RegexOption.IGNORE_CASE
                        )
                            .findAll(documento)
                            .count()

                    val trayectos =
                        Regex(
                            "<gx:Track\\b",
                            RegexOption.IGNORE_CASE
                        )
                            .findAll(documento)
                            .count()

                    listasEncontradas.add(
                        ListaKml(
                            nombre,
                            documento,
                            marcadores,
                            trayectos
                        )
                    )
                }

                // ====================================================
                // ORDEN ALFABÉTICO
                // ====================================================

                listasEncontradas =
                    listasEncontradas
                        .sortedBy {
                            it.nombre.lowercase()
                        }
                        .toMutableList()

                // ====================================================
                // MOSTRAR RESULTADO
                // ====================================================

                listaContenedor.removeAllViews()

                resultado.text =
                    "✅ KMZ leído correctamente\n\n" +
                    "📁 LISTAS ENCONTRADAS: " +
                    listasEncontradas.size

                for (
                    indice in listasEncontradas.indices
                ) {

                    crearBloqueLista(indice)
                }

                boton.text =
                    "📂 SELECCIONAR OTRO KMZ"

            } catch (e: Exception) {

                resultado.text =
                    "❌ Error al analizar el KMZ:\n\n" +
                    e.message
            }
        }

        // ============================================================
        // GUARDAR UNA LISTA
        // ============================================================

        if (
            requestCode == 200 &&
            resultCode == RESULT_OK
        ) {

            val destino = data?.data

            if (
                destino == null ||
                listaParaGuardar < 0 ||
                listaParaGuardar >= listasEncontradas.size
            ) {
                return
            }

            try {

                val lista =
                    listasEncontradas[
                        listaParaGuardar
                    ]

                val salida =
                    contentResolver
                        .openOutputStream(destino)

                if (salida == null) {

                    resultado.text =
                        "❌ No se pudo crear el archivo"

                    return
                }

                val zipSalida =
                    ZipOutputStream(salida)

                // ====================================================
                // CREAR KML INDEPENDIENTE
                // ====================================================

                val kml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\" " +
                    "xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n" +
                    lista.documento +
                    "\n</kml>"

                // ====================================================
                // NOMBRE DEL KML
                // ====================================================

                val nombreKml =
                    nombreArchivoSeguro(
                        lista.nombre
                    ) + ".kml"

                val entradaKml =
                    ZipEntry(nombreKml)

                zipSalida.putNextEntry(
                    entradaKml
                )

                zipSalida.write(
                    kml.toByteArray(
                        Charsets.UTF_8
                    )
                )

                zipSalida.closeEntry()

                zipSalida.close()
                salida.close()

                resultado.text =
                    "✅ LISTA GUARDADA\n\n" +
                    "📁 ${lista.nombre}\n\n" +
                    "📍 Marcadores: " +
                    lista.marcadores +
                    "\n🚶 Trayectos: " +
                    lista.trayectos +
                    "\n\n" +
                    "El KMZ es independiente y " +
                    "conserva el contenido de esta lista."

            } catch (e: Exception) {

                resultado.text =
                    "❌ Error al guardar:\n\n" +
                    e.message
            }
        }
    }

    // ================================================================
    // CREAR BLOQUE VISUAL PARA CADA LISTA
    // ================================================================

    private fun crearBloqueLista(
        indice: Int
    ) {

        val lista =
            listasEncontradas[indice]

        val separador =
            TextView(this)

        separador.text =
            "────────────────────────"

        separador.textSize = 16f
        separador.gravity = Gravity.CENTER

        val nombre =
            TextView(this)

        nombre.text =
            "📁 ${lista.nombre}"

        nombre.textSize = 20f
        nombre.setTypeface(
            null,
            Typeface.BOLD
        )

        nombre.gravity =
            Gravity.CENTER

        nombre.setPadding(
            0,
            20,
            0,
            10
        )

        val datos =
            TextView(this)

        datos.text =
            "📍 Marcadores: ${lista.marcadores}\n" +
            "🚶 Trayectos: ${lista.trayectos}"

        datos.textSize = 17f
        datos.gravity =
            Gravity.CENTER

        val guardar =
            TextView(this)

        guardar.text =
            "\n   💾 GUARDAR ESTA LISTA   \n"

        guardar.textSize = 19f
        guardar.gravity =
            Gravity.CENTER

        guardar.setPadding(
            20,
            20,
            20,
            20
        )

        guardar.setOnClickListener {

            listaParaGuardar =
                indice

            val intent =
                android.content.Intent(
                    android.content.Intent.ACTION_CREATE_DOCUMENT
                )

            intent.type =
                "application/vnd.google-earth.kmz"

            intent.putExtra(
                android.content.Intent.EXTRA_TITLE,
                nombreArchivoSeguro(
                    lista.nombre
                ) + ".kmz"
            )

            startActivityForResult(
                intent,
                200
            )
        }

        listaContenedor.addView(
            separador
        )

        listaContenedor.addView(
            nombre
        )

        listaContenedor.addView(
            datos
        )

        listaContenedor.addView(
            guardar
        )
    }

    // ================================================================
    // LIMPIAR NOMBRES PARA UTILIZARLOS COMO ARCHIVO
    // ================================================================

    private fun nombreArchivoSeguro(
        nombre: String
    ): String {

        return nombre
            .replace("/", "-")
            .replace("\\", "-")
            .replace(":", "-")
            .replace("*", "-")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "-")
            .trim()
    }
}
