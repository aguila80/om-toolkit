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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MainActivity : Activity() {

    lateinit var boton: TextView
    lateinit var resultado: TextView
    lateinit var listaContenedor: LinearLayout

    data class EntradaZip(
        val nombre: String,
        val datos: ByteArray
    )

    data class ListaKml(
        val nombre: String,
        val documento: String,
        val marcadores: Int,
        val trayectos: Int
    )

    var entradasOriginales = mutableListOf<EntradaZip>()

    var nombreKmlOriginal = "OrganicMaps.kml"

    var cabeceraKml = ""
    var cierreKml = ""

    var listasEncontradas = mutableListOf<ListaKml>()

    var listaSeleccionada: ListaKml? = null

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
        version.text = "VERSIÓN 1.0"
        version.textSize = 24f
        version.setTextColor(Color.rgb(0, 140, 70))
        version.gravity = Gravity.CENTER
        version.setPadding(0, 30, 0, 30)

        val texto = TextView(this)
        texto.text = "Separador de listas Organic Maps"
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
        resultado.setPadding(10, 30, 10, 20)

        listaContenedor = LinearLayout(this)
        listaContenedor.orientation = LinearLayout.VERTICAL

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

            val archivo = data?.data ?: return

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

                entradasOriginales.clear()

                val zip =
                    ZipInputStream(
                        ByteArrayInputStream(bytes)
                    )

                var entradaZip =
                    zip.nextEntry

                var contenidoKml = ""

                while (entradaZip != null) {

                    val datos =
                        zip.readBytes()

                    entradasOriginales.add(
                        EntradaZip(
                            entradaZip.name,
                            datos
                        )
                    )

                    if (
                        entradaZip.name
                            .lowercase()
                            .endsWith(".kml")
                    ) {

                        nombreKmlOriginal =
                            entradaZip.name

                        contenidoKml =
                            String(
                                datos,
                                Charsets.UTF_8
                            )
                    }

                    entradaZip =
                        zip.nextEntry
                }

                zip.close()

                if (contenidoKml.isEmpty()) {

                    resultado.text =
                        "❌ No se encontró ningún KML"

                    return
                }

                // ====================================================
                // CONSERVAR EXACTAMENTE LA CABECERA DEL KML
                // ====================================================

                val kmlInicio =
                    Regex(
                        "<kml\\b[^>]*>",
                        RegexOption.IGNORE_CASE
                    )
                        .find(contenidoKml)

                if (kmlInicio == null) {

                    resultado.text =
                        "❌ KML no válido"

                    return
                }

                cabeceraKml =
                    contenidoKml.substring(
                        0,
                        kmlInicio.range.last + 1
                    )

                cierreKml = "</kml>"

                // ====================================================
                // EXTRAER DOCUMENTS
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
                        "❌ No se encontraron listas"

                    return
                }

                listasEncontradas.clear()

                // ====================================================
                // ANALIZAR CADA DOCUMENT
                // ====================================================

                for (documento in documentos) {

                    val nombres =
                        Regex(
                            "<name>([\\s\\S]*?)</name>",
                            RegexOption.IGNORE_CASE
                        )
                            .findAll(documento)
                            .map {
                                it.groupValues[1].trim()
                            }
                            .toList()

                    val nombre =
                        nombres.firstOrNull()
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
                // MOSTRAR LISTAS
                // ====================================================

                listaContenedor.removeAllViews()

                resultado.text =
                    "✅ KMZ leído correctamente\n\n" +
                    "📁 LISTAS ENCONTRADAS: " +
                    listasEncontradas.size +
                    "\n\nSelecciona la lista que quieres guardar."

                for (lista in listasEncontradas) {

                    mostrarLista(lista)
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
        // GUARDAR LISTA
        // ============================================================

        if (
            requestCode == 200 &&
            resultCode == RESULT_OK
        ) {

            val destino =
                data?.data

            val lista =
                listaSeleccionada

            if (
                destino == null ||
                lista == null
            ) {

                resultado.text =
                    "❌ No hay ninguna lista seleccionada"

                return
            }

            try {

                val salida =
                    contentResolver
                        .openOutputStream(destino)

                if (salida == null) {

                    resultado.text =
                        "❌ No se pudo crear el archivo"

                    return
                }

                // ====================================================
                // ELIMINAR gx:Track DUPLICADOS IDÉNTICOS
                // ====================================================

                val documentoLimpio =
    eliminarTracksDuplicados(
        lista.documento
    )

                // ====================================================
                // CONSTRUIR KML FINAL
                //
                // SOLO:
                // cabecera ORIGINAL
                // +
                // Document ORIGINAL
                // +
                // cierre ORIGINAL
                //
                // No creamos un Document nuevo.
                // ====================================================

                val kmlFinal =
                    cabeceraKml +
                    "\n" +
                    documentoLimpio +
                    "\n" +
                    cierreKml +
                    "\n"

                // ====================================================
                // CREAR ZIP
                //
                // Conservamos todos los archivos originales
                // y sustituimos únicamente el KML.
                // ====================================================

                val zipSalida =
                    ZipOutputStream(salida)

                for (
                    entradaOriginal
                    in entradasOriginales
                ) {

                    val esKml =
                        entradaOriginal.nombre
                            .lowercase()
                            .endsWith(".kml")

                    if (esKml) {

                        val nuevaEntrada =
                            ZipEntry(
                                entradaOriginal.nombre
                            )

                        zipSalida.putNextEntry(
                            nuevaEntrada
                        )

                        zipSalida.write(
                            kmlFinal.toByteArray(
                                Charsets.UTF_8
                            )
                        )

                        zipSalida.closeEntry()

                    } else {

                        val nuevaEntrada =
                            ZipEntry(
                                entradaOriginal.nombre
                            )

                        zipSalida.putNextEntry(
                            nuevaEntrada
                        )

                        zipSalida.write(
                            entradaOriginal.datos
                        )

                        zipSalida.closeEntry()
                    }
                }

                zipSalida.close()
                salida.close()

                // ====================================================
                // CONTAR LO QUE REALMENTE HEMOS GUARDADO
                // ====================================================

                val puntosFinales =
                    Regex(
                        "<Point\\b",
                        RegexOption.IGNORE_CASE
                    )
                        .findAll(
                            documentoLimpio
                        )
                        .count()

                val tracksFinales =
                    Regex(
                        "<gx:Track\\b",
                        RegexOption.IGNORE_CASE
                    )
                        .findAll(
                            documentoLimpio
                        )
                        .count()

                resultado.text =
                    "✅ KMZ GUARDADO\n\n" +
                    "📁 ${lista.nombre}\n\n" +
                    "📍 Marcadores: $puntosFinales\n" +
                    "🚶 Trayectos: $tracksFinales\n\n" +
                    "La estructura original del KML " +
                    "se ha conservado."

            } catch (e: Exception) {

                resultado.text =
                    "❌ Error al guardar el KMZ:\n\n" +
                    e.message
            }
        }
    }

    // ================================================================
    // MOSTRAR LISTA
    // ================================================================

    private fun mostrarLista(
        lista: ListaKml
    ) {

        val separador =
            TextView(this)

        separador.text =
            "────────────────────────"

        separador.gravity =
            Gravity.CENTER

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

            listaSeleccionada =
                lista

            resultado.text =
                "📌 LISTA SELECCIONADA\n\n" +
                "📁 ${lista.nombre}\n" +
                "📍 ${lista.marcadores} marcadores\n" +
                "🚶 ${lista.trayectos} trayectos"

            val intent =
                android.content.Intent(
                    android.content.Intent.ACTION_CREATE_DOCUMENT
                )

            intent.type =
                "application/vnd.google-earth.kmz"

            intent.putExtra(
                android.content.Intent.EXTRA_TITLE,
                nombreSeguro(
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
    // ELIMINAR SOLO gx:Track COMPLETAMENTE IDÉNTICOS
    // ================================================================

    private fun eliminarTracksDuplicados(
        documento: String
    ): String {

        val tracks =
            Regex(
                "<gx:Track\\b[\\s\\S]*?</gx:Track>",
                RegexOption.IGNORE_CASE
            )
                .findAll(documento)
                .map {
                    it.value
                }
                .toList()

        if (tracks.size <= 1) {
            return documento
        }

        val vistos =
            mutableSetOf<String>()

        var resultadoDocumento =
            documento

        for (track in tracks) {

            if (vistos.contains(track)) {

                resultadoDocumento =
                    resultadoDocumento.replaceFirst(
                        track,
                        ""
                    )

            } else {

                vistos.add(track)
            }
        }

        return resultadoDocumento
    }
// ================================================================
// LIMPIAR MARCADORES DUPLICADOS DENTRO DE CADA LISTA
// ================================================================


    // ================================================================
    // LIMPIAR NOMBRE DE ARCHIVO
    // ================================================================

    private fun nombreSeguro(
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
