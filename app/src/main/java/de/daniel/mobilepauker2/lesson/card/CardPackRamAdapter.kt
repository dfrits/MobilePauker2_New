package de.daniel.mobilepauker2.lesson.card

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.utils.Log
import javax.inject.Inject

class CardPackRamAdapter(context: Context) : CardPackAdapter(context) {
    val isLastCard: Boolean
        get() = cardCursor.isLast()

    @Inject
    lateinit var lessonManager: LessonManager
    private val cardCursor: FlashCardCursor

    override fun open(): CardPackAdapter {
        return this
    }

    override fun close() {
        cardCursor.close()
    }

    @Throws(CursorIndexOutOfBoundsException::class)
    override fun deleteFlashCard(cardId: Long): Boolean {
        val position: Int = cardCursor.getPosition()
        val returnVal: Boolean
        var requestFirst = false
        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }
        if (position >= lessonManager.getBatchSize(BatchType.CURRENT)) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }

        Log.d("CardPackRamAdapter::deleteFlashCard", "CardCount - " + cardCursor.count)
        if (cardCursor.isFirst()) {
            requestFirst = true
        } else {
            cardCursor.moveToPrevious()
        }
        returnVal = lessonManager.deleteCard(position)

        // Point to the first card if
        // * We deleted second last card (size now is 1)
        // * We have just deleted the first card
        if (cardCursor.count == 1 || requestFirst) {
            cardCursor.moveToFirst()
        }
        return returnVal
    }

    override fun fetchAllFlashCards(): Cursor {
        return cardCursor
    }

    override fun countCardsInTable(): Int {
        return cardCursor.count
    }

    fun setCardLearned() {
        lessonManager.putCardToNextBatch(cardCursor.getPosition())
    }

    fun setCardUnLearned() {
        lessonManager.moveCardToUnlearndBatch(cardCursor.getPosition())
    }

    init {
        cardCursor = FlashCardCursor()
    }
}