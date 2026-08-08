 package com.omtoolkit.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 50, 32, 32)
        }

        val title = TextView(this).apply {
            text = "OM Toolkit"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Herramientas para Organic Maps"
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 40)
        }

        root.addView(title)
        root.addView(subtitle)

        addButton(root, "Abrir KML / KMZ")
        addButton(root, "Ordenar marcadores A–Z")
        addButton(root, "Reordenar manualmente")
        addButton(root, "Guardar KML")

        setContentView(root)
    }

    private fun addButton(parent: ViewGroup, text: String) {
        val button = Button(this).apply {
            this.text = text
            textSize = 16f
            isAllCaps = false
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 8, 0, 8)
        parent.addView(button, params)
    }
}
