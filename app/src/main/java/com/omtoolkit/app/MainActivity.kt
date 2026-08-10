package com.omtoolkit.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class MainActivity : Activity() {

    lateinit var boton: TextView
    lateinit var guardar: TextView
    lateinit var resultado: TextView

    var kmlOrdenado: String = ""
    var nombreKml: String = "doc.kml"

    // Conservamos todos los archivos del KMZ original
    var entradasOriginales =
        mutableListOf<Pair<String, ByteArray>>()

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
        version.text = "VERSIÓN 0.5"
        version.textSize = 24f
        version.setTextColor(Color.rgb(0, 140, 70))
        version.gravity = Gravity.CENTER
        version.setPadding(0, 30, 0, 30)

        val texto = TextView(this)
        texto.text = "Ordenador de listas Organic Maps"
        texto.textSize = 20f
        texto.gravity = Gravity.CENTER

        boton = TextView(this)
        boton.text = "\n   📂  SELECCIONAR KMZ   \n"
        boton.textSize = 20f
        boton.gravity = Gravity.CENTER
        boton.setPadding(20, 30, 20, 30)

        guardar = TextView(this)
        guardar.text = "\n   💾  GUARDAR KMZ ORDENADO   \n"
        guardar.textSize = 20f
        guardar.gravity = Gravity.CENTER
        guardar.setPadding(20, 30, 20, 30)
        guardar.visibility = TextView.GONE

        resultado = TextView(this)
        resultado.text = ""
        resultado.textSize = 18f
        resultado.gravity = Gravity.CENTER
        resultado.setPadding(10, 40, 10, 10)

        pantalla.addView(titulo)
        pantalla.addView(version)
        pantalla.addView(texto)
        pantalla.addView(boton)
        pantalla.addView(guardar)
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

        guardar.setOnClickListener {

            if (kmlOrdenado.isEmpty()) {

                resultado.text =
                    "❌ No hay ningún KMZ ordenado para guardar."

                return@setOnClickListener
            }

            val intent = android.content.Intent(
                android.content.Intent.ACTION_CREATE_DOCUMENT
            )

            intent.type = "application/vnd.google-earth.kmz"

            intent.putExtra(
                android.content.Intent.EXTRA_TITLE,
                "OrganicMaps_ordenado.kmz"
            )

            startActivityForResult(intent, 200)
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
        // SELECCIONAR KMZ
        // ============================================================

        if (
            requestCode == 100 &&
            resultCode == RESULT_OK
        ) {

            val archivo = data?.data

            if (archivo != null) {

                try {

                    val entradaOriginal =
                        contentResolver.openInputStream(archivo)

                    if (entradaOriginal == null) {

                        resultado.text =
                            "❌ No se pudo abrir el KMZ"

                        return
                    }

                    val bytesOriginales =
                        entradaOriginal.readBytes()

                    entradaOriginal.close()

                    // Guardamos todos los archivos del KMZ
                    entradasOriginales.clear()

                    val zip =
                        ZipInputStream(
                            ByteArrayInputStream(
                                bytesOriginales
                            )
                        )

                    var contenidoKml = ""
                    var nombreEntradaKml = "doc.kml"

                    var entradaZip = zip.nextEntry

                    while (entradaZip != null) {

                        val datos =
                            zip.readBytes()

                        entradasOriginales.add(
                            Pair(
                                entradaZip.name,
                                datos
                            )
                        )

                        if (
                            entradaZip.name
                                .lowercase()
                                .endsWith(".kml")
                        ) {

                            nombreEntradaKml =
                                entradaZip.name

                            contenidoKml =
                                String(
                                    datos,
                                    Charsets.UTF_8
                                )
                        }

                        entradaZip = zip.nextEntry
                    }

                    zip.close()

                    if (contenidoKml.isEmpty()) {

                        resultado.text =
                            "❌ No se encontró ningún archivo KML dentro del KMZ"

                        return
                    }

                    nombreKml =
                        nombreEntradaKml

                    // ====================================================
                    // BUSCAR LOS DOCUMENT
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

                    // ====================================================
                    // OBTENER NOMBRE DE CADA LISTA
                    // ====================================================

                    val listas =
                        documentos.map { documento ->

                            Regex(
                                "<name>(.*?)</name>",
                                RegexOption.IGNORE_CASE
                            )
                                .find(documento)
                                ?.groupValues
                                ?.get(1)
                                ?.trim()
                                ?: ""

                        }.toList()

                    // ====================================================
                    // ORDEN ALFABÉTICO
                    // ====================================================

                    val listasOrdenadas =
                        documentos
                            .zip(listas)
                            .sortedBy {
                                it.second.lowercase()
                            }

                    // ====================================================
                    // CONTAR MARCADORES
                    // ====================================================

                    val marcadores =
                        Regex(
                            "<Point\\b",
                            RegexOption.IGNORE_CASE
                        )
                            .findAll(contenidoKml)
                            .count()

                    // ====================================================
                    // CONTAR TRAYECTOS
                    // ====================================================

                    val trayectos =
                        Regex(
                            "<gx:Track\\b",
                            RegexOption.IGNORE_CASE
                        )
                            .findAll(contenidoKml)
                            .count()

                    // ====================================================
                    // RECONSTRUIR KML ORDENADO
                    // ====================================================

                    if (documentos.size > 1) {

                        val primerDocumento =
                            contenidoKml.indexOf(
                                documentos.first()
                            )

                        val ultimoDocumento =
                            contenidoKml.lastIndexOf(
                                documentos.last()
                            )

                        if (
                            primerDocumento >= 0 &&
                            ultimoDocumento >= 0
                        ) {

                            val antes =
                                contenidoKml.substring(
                                    0,
                                    primerDocumento
                                )

                            val despues =
                                contenidoKml.substring(
                                    ultimoDocumento +
                                        documentos.last().length
                                )

                            val documentosNuevos =
                                listasOrdenadas
                                    .joinToString("\n") {
                                        it.first
                                    }

                            kmlOrdenado =
                                antes +
                                documentosNuevos +
                                despues

                        } else {

                            kmlOrdenado =
                                contenidoKml
                        }

                    } else {

                        kmlOrdenado =
                            contenidoKml
                    }

                    // ====================================================
                    // MOSTRAR RESULTADO
                    // ====================================================

                    resultado.text =
                        "✅ KMZ leído correctamente\n\n" +
                        "📄 $nombreEntradaKml\n\n" +
                        "📍 Marcadores: $marcadores\n" +
                        "🚶 Trayectos: $trayectos\n\n" +
                        "📁 LISTAS: ${listas.size}\n\n" +
                        listasOrdenadas.joinToString("\n") {
                            it.second
                        } +
                        "\n\n" +
                        "El original NO ha sido modificado."

                    guardar.visibility =
                        TextView.VISIBLE

                    boton.text =
                        "📂 SELECCIONAR OTRO KMZ"

                } catch (e: Exception) {

                    resultado.text =
                        "❌ Error al analizar el KMZ:\n\n" +
                        e.message
                }
            }
        }

        // ============================================================
        // GUARDAR KMZ
        // ============================================================

        if (
            requestCode == 200 &&
            resultCode == RESULT_OK
        ) {

            val destino = data?.data

            if (destino != null) {

                try {

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
                    // RECONSTRUIR TODO EL KMZ ORIGINAL
                    // ====================================================

                    for (
                        entrada in entradasOriginales
                    ) {

                        val nombre =
                            entrada.first

                        val datos =
                            entrada.second

                        val zipEntry =
                            ZipEntry(nombre)

                        zipSalida.putNextEntry(
                            zipEntry
                        )

                        if (
                            nombre == nombreKml
                        ) {

                            zipSalida.write(
                                kmlOrdenado.toByteArray(
                                    Charsets.UTF_8
                                )
                            )

                        } else {

                            zipSalida.write(datos)
                        }

                        zipSalida.closeEntry()
                    }

                    zipSalida.close()
                    salida.close()

                    resultado.text =
                        "✅ KMZ ORDENADO GUARDADO\n\n" +
                        "📁 Listas: ${listasOrdenadasTexto()}\n\n" +
                        "📦 Se han conservado todos los archivos " +
                        "del KMZ original.\n\n" +
                        "📍 Marcadores y trayectos conservados."

                    guardar.visibility =
                        TextView.GONE

                } catch (e: Exception) {

                    resultado.text =
                        "❌ Error al guardar el KMZ:\n\n" +
                        e.message
                }
            }
        }
    }

    // ================================================================
    // TEXTO AUXILIAR PARA MOSTRAR LAS LISTAS
    // ================================================================

    private fun listasOrdenadasTexto(): String {

        val documentos =
            Regex(
                "<Document\\b[\\s\\S]*?</Document>",
                RegexOption.IGNORE_CASE
            )
                .findAll(kmlOrdenado)
                .map {
                    it.value
                }
                .toList()

        return documentos
            .map { documento ->

                Regex(
                    "<name>(.*?)</name>",
                    RegexOption.IGNORE_CASE
                )
                    .find(documento)
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?: ""

            }
            .joinToString(" → ")
    }
}
