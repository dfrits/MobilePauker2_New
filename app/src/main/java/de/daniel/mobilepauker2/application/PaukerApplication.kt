package de.daniel.mobilepauker2.application

import android.app.Application

class PaukerApplication : Application() {
    lateinit var applicationSingletonComponent: ApplicationSingletonComponent

    override fun onCreate() {
        super.onCreate()

        applicationSingletonComponent = DaggerApplicationSingletonComponent.builder()
            .application(this)
            .managerModule(this)
            .build()
    }
}