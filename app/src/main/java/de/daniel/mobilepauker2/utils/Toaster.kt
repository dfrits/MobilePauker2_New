package de.daniel.mobilepauker2.utils

import android.app.Activity
import android.widget.Toast

class Toaster {
    companion object {
        fun showToast(context: Activity, text: String?, duration: Int) {
            context.runOnUiThread {
                if (text != null && text.isNotEmpty()) {
                    Toast.makeText(context, text, duration).show()
                }
            }
        }

        fun showToast(context: Activity, textResource: Int, duration: Int) {
            showToast(context, context.getString(textResource), duration)
        }
    }
}