package com.example.myapplication.utils.extension

import android.graphics.Paint
import android.os.Build
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import com.example.myapplication.R

fun NumberPicker.setBrownTextColor() {
    val color = ContextCompat.getColor(context, R.color.brown)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textColor = color
    }
    try {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child is EditText) {
                child.setTextColor(color)
            }
        }
        val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
        selectorWheelPaintField.isAccessible = true
        (selectorWheelPaintField.get(this) as? Paint)?.color = color
        invalidate()
    } catch (e: Exception) {
        // Ignore reflection errors
    }
}
