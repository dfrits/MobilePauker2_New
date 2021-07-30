package de.daniel.mobilepauker2.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(R.id.content, SettingsFragment()).commit()
    }
}