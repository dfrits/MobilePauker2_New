package de.daniel.mobilepauker2.application

import android.app.Application
import de.daniel.mobilepauker2.statistics.ChartAdapter

class PaukerApplication: Application() {
    val appComponent = DaggerApplicationComponent.create()
    val appSingletonComponent = DaggerApplicationSingletonComponent.create()

    fun inject(chartAdapter: ChartAdapter) {

    }
}