 package com.omtoolkit.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

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
        version.text = "VERSIÓN 0.2"
        version.textSize = 24f
        version.setTextColor(Color.rgb(0, 140, 70))
        version.gravity = Gravity.CENTER
        version.setPadding(0, 30, 0, 30)

        val texto = TextView(this)
        texto.text = "Herramientas para Organic Maps"
        texto.textSize = 20f
        texto.gravity = Gravity.CENTER

        val boton = TextView(this)
        boton.text = "\n   🔤  ORDENAR MARCADORES   \n"
        boton.textSize = 20f
        boton.gravity = Gravity.CENTER
        boton.setPadding(20, 30, 20, 30)

        pantalla.addView(titulo)
        pantalla.addView(version)
        pantalla.addView(texto)
        pantalla.addView(boton)
                boton.setOnClickListener {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_OPEN_DOCUMENT
    )
    intent.type = "*/*"
    intent.addCategory(
        android.content.Intent.CATEGORY_OPENABLE
    )
    startActivityForResult(intent, 100)
}

        setContentView(pantalla)
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
                boton.text = "📂 KMZ seleccionado"
            }
        }
    }
}}
