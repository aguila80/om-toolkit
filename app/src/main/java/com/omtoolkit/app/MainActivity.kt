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
        val documento: String
    )

    var entradasOriginales =
        mutableListOf<EntradaZip>()

    var listasEncontradas =
        mutableListOf<ListaKml>()

    var duplicadosZip =
        mutableListOf<String>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val scroll =
            ScrollView(this)

        val pantalla =
            LinearLayout(this)

        pantalla.orientation =
            LinearLayout.VERTICAL

        pantalla.gravity =
            Gravity.CENTER

        pantalla.setPadding(
            40,
            40,
            40,
            40
        )

        val titulo =
            TextView(this)

        titulo.text =
            "OM Toolkit"

        titulo.textSize =
            32f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        val version =
            TextView(this)

        version.text =
            "VERSIÓN 1.3"

        version.textSize =
            24f

        version.setTextColor(
            Color.rgb(0, 140, 70)
        )

        version.gravity =
            Gravity.CENTER

        version.setPadding(
            0,
            30,
            0,
            30
        )

        val texto =
            TextView(this)

        texto.text =
            "Diagnóstico ZIP de Organic Maps"

        texto.textSize =
            20f

        texto.gravity =
            Gravity.CENTER

        boton =
            TextView(this)

        boton.text =
            "\n   📂  SELECCIONAR BACKUP KMZ   \n"

        boton.textSize =
            20f

        boton.gravity =
            Gravity.CENTER

        boton.setPadding(
            20,
            30,
            20,
            30
        )

        resultado =
            TextView(this)

        resultado.text =
            ""

        resultado.textSize =
            18f

        resultado.gravity =
            Gravity.CENTER

        resultado.setPadding(
            10,
            30,
            10,
            20
        )

        listaContenedor =
            LinearLayout(this)

        listaContenedor.orientation =
            LinearLayout.VERTICAL

        pantalla.addView(titulo)
        pantalla.addView(version)
        pantalla.addView(texto)
        pantalla.addView(boton)
        pantalla.addView(resultado)
        pantalla.addView(listaContenedor)

        boton.setOnClickListener {

            val intent =
                android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT
                )

            intent.type =
                "application/vnd.google-earth.kmz"

            intent.addCategory(
                android.content.Intent.CATEGORY_OPENABLE
            )

            startActivityForResult(
                intent,
                100
            )
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
        // LEER KMZ
        // ============================================================

        if (
            requestCode == 100 &&
            resultCode == RESULT_OK
        ) {

            val archivo =
                data?.data ?: return

            try {

                val entrada =
                    contentResolver
                        .openInputStream(archivo)

                if (entrada == null) {

                    resultado.text =
                        "❌ No se pudo abrir el KMZ"

                    return
                }

                val bytes =
                    entrada.readBytes()

                entrada.close()

                entradasOriginales.clear()
                listasEncontradas.clear()
                duplicadosZip.clear()

                // ====================================================
                // LEER TODAS LAS ENTRADAS DEL ZIP
                // ====================================================

                val nombresYaLeidos =
                    mutableSetOf<String>()

                val zip =
                    ZipInputStream(
                        ByteArrayInputStream(bytes)
                    )

                var entradaZip =
                    zip.nextEntry

                var totalEntradasLeidas =
                    0

                while (
                    entradaZip != null
                ) {

                    val nombre =
                        entradaZip.name

                    val datos =
                        zip.readBytes()

                    totalEntradasLeidas++

                    // ================================================
                    // DETECTAR NOMBRE DUPLICADO
                    // ================================================

                    if (
                        nombresYaLeidos.contains(
                            nombre
                        )
                    ) {

                        duplicadosZip.add(
                            nombre
                        )
                    }

                    nombresYaLeidos.add(
                        nombre
                    )

                    // ================================================
                    // CONSERVAMOS LA ENTRADA
                    // ================================================

                    entradasOriginales.add(
                        EntradaZip(
                            nombre,
                            datos
                        )
                    )

                    entradaZip =
                        zip.nextEntry
                }

                zip.close()

                // ====================================================
                // BUSCAR LISTAS KML
                // ====================================================

                val archivosKml =
                    entradasOriginales.filter {

                        val nombre =
                            it.nombre.lowercase()

                        nombre.endsWith(".kml") &&
                        !nombre.substringAfterLast("/")
                            .equals(
                                "doc.kml",
                                ignoreCase = true
                            )
                    }

                for (
                    archivoKml
                    in archivosKml
                ) {

                    val contenido =
                        String(
                            archivoKml.datos,
                            Charsets.UTF_8
                        )

                    val documento =
                        Regex(
                            "<Document\\b[\\s\\S]*?</Document>",
                            RegexOption.IGNORE_CASE
                        )
                            .find(
                                contenido
                            )
                            ?.value

                    if (
                        documento != null
                    ) {

                        val nombre =
                            Regex(
                                "<name>([\\s\\S]*?)</name>",
                                RegexOption.IGNORE_CASE
                            )
                                .find(
                                    documento
                                )
                                ?.groupValues
                                ?.get(1)
                                ?.trim()
                                ?: archivoKml.nombre
                                    .substringAfterLast("/")
                                    .removeSuffix(".kml")

                        listasEncontradas.add(
                            ListaKml(
                                nombre,
                                documento
                            )
                        )
                    }
                }

                // ====================================================
                // ORDEN SOLO VISUAL
                // ====================================================

                listasEncontradas =
                    listasEncontradas
                        .sortedBy {
                            it.nombre.lowercase()
                        }
                        .toMutableList()

                // ====================================================
                // MOSTRAR DIAGNÓSTICO
                // ====================================================

                listaContenedor.removeAllViews()

                var mensaje =
                    "✅ KMZ LEÍDO CORRECTAMENTE\n\n" +
                    "📁 Listas encontradas: " +
                    listasEncontradas.size +
                    "\n\n" +
                    "📦 Entradas ZIP leídas: " +
                    totalEntradasLeidas +
                    "\n\n" +
                    "🔎 Nombres ZIP duplicados: " +
                    duplicadosZip.size

                if (
                    duplicadosZip.isNotEmpty()
                ) {

                    mensaje +=
                        "\n\n⚠️ PRIMEROS DUPLICADOS:\n"

                    duplicadosZip
                        .take(5)
                        .forEach {

                            mensaje +=
                                "\n• " + it
                        }
                }

                mensaje +=
                    "\n\n💾 Pulsa GUARDAR para " +
                    "hacer la prueba de reconstrucción."

                resultado.text =
                    mensaje

                // ====================================================
                // BOTÓN GUARDAR
                // ====================================================

                val guardar =
                    TextView(this)

                guardar.text =
                    "\n   💾 GUARDAR KMZ DE PRUEBA   \n"

                guardar.textSize =
                    20f

                guardar.gravity =
                    Gravity.CENTER

                guardar.setPadding(
                    20,
                    30,
                    20,
                    30
                )

                guardar.setOnClickListener {

                    val intent =
                        android.content.Intent(
                            android.content.Intent.ACTION_CREATE_DOCUMENT
                        )

                    intent.type =
                        "application/vnd.google-earth.kmz"

                    intent.putExtra(
                        android.content.Intent.EXTRA_TITLE,
                        "OrganicMaps_diagnostico_c.kmz"
                    )

                    startActivityForResult(
                        intent,
                        200
                    )
                }

                listaContenedor.addView(
                    guardar
                )

                boton.text =
                    "📂 SELECCIONAR OTRO BACKUP KMZ"

            } catch (
                e: Exception
            ) {

                resultado.text =
                    "❌ ERROR AL LEER KMZ\n\n" +
                    e.message
            }
        }

        // ============================================================
        // GUARDAR KMZ DE DIAGNÓSTICO
        // ============================================================

        if (
            requestCode == 200 &&
            resultCode == RESULT_OK
        ) {

            val destino =
                data?.data

            if (
                destino == null
            ) {

                resultado.text =
                    "❌ No se seleccionó destino"

                return
            }

            try {

                val salida =
                    contentResolver
                        .openOutputStream(destino)

                if (
                    salida == null
                ) {

                    resultado.text =
                        "❌ No se pudo crear el archivo"

                    return
                }

                val zipSalida =
                    ZipOutputStream(
                        salida
                    )

                val nombresEscritos =
                    mutableSetOf<String>()

                var entradasGuardadas =
                    0

                var duplicadosOmitidos =
                    0

                // ====================================================
                // COPIAR LAS ENTRADAS
                // ====================================================

                for (
                    entradaOriginal
                    in entradasOriginales
                ) {

                    val nombreOriginal =
                        entradaOriginal.nombre

                    // =================================================
                    // ZIP NO ADMITE DOS ENTRADAS CON EL MISMO NOMBRE
                    // =================================================

                    if (
                        nombresEscritos.contains(
                            nombreOriginal
                        )
                    ) {

                        duplicadosOmitidos++

                        continue
                    }

                    val nuevaEntrada =
                        ZipEntry(
                            nombreOriginal
                        )

                    zipSalida.putNextEntry(
                        nuevaEntrada
                    )

                    zipSalida.write(
                        entradaOriginal.datos
                    )

                    zipSalida.closeEntry()

                    nombresEscritos.add(
                        nombreOriginal
                    )

                    entradasGuardadas++
                }

                zipSalida.close()
                salida.close()

                // ====================================================
                // RESULTADO
                // ====================================================

                resultado.text =
                    "✅ KMZ DE DIAGNÓSTICO GUARDADO\n\n" +
                    "📁 Listas encontradas: " +
                    listasEncontradas.size +
                    "\n\n" +
                    "📦 Entradas ZIP leídas: " +
                    entradasOriginales.size +
                    "\n\n" +
                    "💾 Entradas ZIP guardadas: " +
                    entradasGuardadas +
                    "\n\n" +
                    "⚠️ Duplicados omitidos: " +
                    duplicadosOmitidos +
                    "\n\n" +
                    "Este KMZ NO ha sido ordenado " +
                    "ni limpiado.\n\n" +
                    "Solo sirve para diagnosticar " +
                    "la estructura del backup."

            } catch (
                e: Exception
            ) {

                resultado.text =
                    "❌ ERROR AL GUARDAR KMZ\n\n" +
                    e.message
            }
        }
    }
}
