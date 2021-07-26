package de.daniel.mobilepauker2.lessonimport

import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.models.NextExpireDateResult
import java.net.URI
import javax.inject.Inject

class LessonImportViewModel @Inject constructor() {
    var lastSelection: Int = -1
        private set

    fun resetSelection() {
        lastSelection = -1
    }

    fun itemClicked(position: Int) {
        if (lastSelection != position) {
            lastSelection = position
        } else {
            resetSelection()
        }
    }

    fun getNextExpireDate(uri: URI): NextExpireDateResult =
        FlashCardXMLPullFeedParser(uri.toURL()).getNextExpireDate()
}