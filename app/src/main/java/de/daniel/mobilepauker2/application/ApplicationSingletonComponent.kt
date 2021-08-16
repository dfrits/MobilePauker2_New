package de.daniel.mobilepauker2.application

import dagger.Component
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.editcard.AbstractEditCard
import de.daniel.mobilepauker2.editcard.AddCard
import de.daniel.mobilepauker2.lesson.EditDescription
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.card.CardPackRamAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCardCursor
import de.daniel.mobilepauker2.lessonimport.LessonImport
import de.daniel.mobilepauker2.lessonimport.LessonImportAdapter
import de.daniel.mobilepauker2.mainmenu.MainMenu
import de.daniel.mobilepauker2.models.view.MPEditText
import de.daniel.mobilepauker2.models.view.MPTextView
import de.daniel.mobilepauker2.search.Search
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
    fun inject(mainMenu: MainMenu)
    fun inject(lessonImport: LessonImport)
    fun inject(lessonImportAdapter: LessonImportAdapter)
    fun inject(mpEditText: MPEditText)
    fun inject(abstractEditCard: AbstractEditCard)
    fun inject(editDescription: EditDescription)
    fun inject(mpTextView: MPTextView)
    fun inject(search: Search)
}