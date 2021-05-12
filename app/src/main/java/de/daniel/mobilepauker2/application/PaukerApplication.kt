package de.daniel.mobilepauker2.application

import android.app.Application

class PaukerApplication: Application() {
    val appComponent = DaggerApplicationComponent.create()
}