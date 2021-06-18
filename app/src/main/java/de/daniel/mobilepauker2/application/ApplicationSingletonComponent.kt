package de.daniel.mobilepauker2.application

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.card.CardPackRamAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCardCursor
import de.daniel.mobilepauker2.statistics.ChartAdapter
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Singleton

@Singleton
@Component(modules = [ProviderModule::class])
interface ApplicationSingletonComponent {

    fun inject(manager: DataManager)
    fun inject(manager: LessonManager)
    fun inject(adapter: CardPackRamAdapter)
    fun inject(adapter: ChartAdapter)
    fun inject(cursor: FlashCardCursor)
    fun inject(utils: Toaster)
}