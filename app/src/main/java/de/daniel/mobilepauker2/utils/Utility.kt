package de.daniel.mobilepauker2.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


class Utility {
    companion object {
        fun Fragment.hideKeyboard() {
            view?.let { activity?.hideKeyboard(it) }
        }

        fun Activity.hideKeyboard() {
            hideKeyboard(currentFocus ?: View(this))
        }

        fun Context.hideKeyboard(view: View) {
            val inputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun Fragment.showKeyboard() {
            view?.let { activity?.showKeyboard(it) }
        }

        fun Activity.showKeyboard() {
            showKeyboard(currentFocus ?: View(this))
        }

        fun Context.showKeyboard(view: View) {
            val imm = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, 0)
        }

        fun isAppRunning(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            val listRunningApps = activityManager?.runningAppProcesses

            listRunningApps?.forEach { runAppProccess ->
                if (runAppProccess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Log.d("current foreground App", runAppProccess.processName)
                }
            }
            return false
        }
    }
}