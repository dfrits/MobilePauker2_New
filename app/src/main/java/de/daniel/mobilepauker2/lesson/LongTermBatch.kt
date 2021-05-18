package de.daniel.mobilepauker2.lesson

import kotlin.math.pow

class LongTermBatch(newBatchIndex: Int) : Batch(mutableListOf()) {
    companion object {
        private val ONE_SECOND: Long = 1000
        private val ONE_MINUTE = ONE_SECOND * 60
        private val ONE_HOUR = ONE_MINUTE * 60
        private val ONE_DAY = ONE_HOUR * 24
        val EXPIRATION_UNIT = ONE_DAY
    }

    val expiredCards = mutableListOf<Card>()
    val expirationTime: Double

    init {
        val factor = Math.E.pow(newBatchIndex)
        expirationTime = EXPIRATION_UNIT * factor
    }

    fun refreshExpiredCards() {
        val currentTime = System.currentTimeMillis()

        expiredCards.clear()

        cards.forEach { card ->
            val learnedTime = card.learnedTimestamp
            val diff = currentTime - learnedTime
            if (diff > expirationTime) {
                expiredCards.add(card)
            }
        }
    }
}