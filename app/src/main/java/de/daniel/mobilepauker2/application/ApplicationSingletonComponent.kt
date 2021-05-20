package de.daniel.mobilepauker2.application

import dagger.Component
import de.daniel.mobilepauker2.statistics.ChartAdapter
import javax.inject.Singleton

@Singleton
@Component
interface ApplicationSingletonComponent {
    fun inject(chartAdapter: ChartAdapter)
}