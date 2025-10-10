package com.mpdc4gsr.module.thermalunified.stubs

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class ThermalInputDialog {
    class Builder(
        private val context: Context,
    ) {
        private var message: String = ""
        private var positiveListener: ((Float, Float, Int, Int) -> Unit)? = null
        private var cancelListener: (() -> Unit)? = null
        private var maxTemp: Float = 100f
        private var minTemp: Float = 0f
        private var maxColor: Int = android.graphics.Color.RED
        private var minColor: Int = android.graphics.Color.BLUE

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setNum(
            max: Float = 100f,
            min: Float = 0f,
        ): Builder {
            this.maxTemp = max
            this.minTemp = min
            return this
        }

        fun setColor(
            maxColor: Int = android.graphics.Color.RED,
            minColor: Int = android.graphics.Color.BLUE,
        ): Builder {
            this.maxColor = maxColor
            this.minColor = minColor
            return this
        }

        fun setPositiveListener(
            textResId: Int,
            listener: (Float, Float, Int, Int) -> Unit,
        ): Builder {
            this.positiveListener = listener
            return this
        }

        fun setCancelListener(textResId: Int): Builder = this

        fun setCancelListener(
            text: String,
            listener: () -> Unit,
        ): Builder {
            this.cancelListener = listener
            return this
        }

        fun create(): ThermalInputDialog =
            ThermalInputDialog().apply {
                this.context = this@Builder.context
                this.message = this@Builder.message
                this.positiveListener = this@Builder.positiveListener
                this.cancelListener = this@Builder.cancelListener
                this.maxTemp = this@Builder.maxTemp
                this.minTemp = this@Builder.minTemp
                this.maxColor = this@Builder.maxColor
                this.minColor = this@Builder.minColor
            }
    }

    private lateinit var context: Context
    private var message: String = ""
    private var positiveListener: ((Float, Float, Int, Int) -> Unit)? = null
    private var cancelListener: (() -> Unit)? = null
    private var maxTemp: Float = 100f
    private var minTemp: Float = 0f
    private var maxColor: Int = android.graphics.Color.RED
    private var minColor: Int = android.graphics.Color.BLUE

    fun show() {
        val layout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 50, 50, 50)
            }
        // Add message
        layout.addView(
            TextView(context).apply {
                text = message
                setPadding(0, 0, 0, 20)
            },
        )
        // Add input fields
        val maxTempEdit =
            EditText(context).apply {
                hint = "Max Temperature"
                setText(maxTemp.toString())
            }
        layout.addView(TextView(context).apply { text = "Max Temperature:" })
        layout.addView(maxTempEdit)
        val minTempEdit =
            EditText(context).apply {
                hint = "Min Temperature"
                setText(minTemp.toString())
            }
        layout.addView(TextView(context).apply { text = "Min Temperature:" })
        layout.addView(minTempEdit)
        AlertDialog
            .Builder(context)
            .setTitle("Thermal Input")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                try {
                    val maxVal = maxTempEdit.text.toString().toFloat()
                    val minVal = minTempEdit.text.toString().toFloat()
                    positiveListener?.invoke(maxVal, minVal, maxColor, minColor)
                } catch (e: NumberFormatException) {
                    // Use default values if parsing fails
                    positiveListener?.invoke(maxTemp, minTemp, maxColor, minColor)
                }
            }.setNegativeButton("Cancel") { _, _ ->
                cancelListener?.invoke()
            }.create()
            .show()
    }
}
