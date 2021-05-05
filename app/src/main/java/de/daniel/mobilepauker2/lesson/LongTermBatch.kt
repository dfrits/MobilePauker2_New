package de.daniel.mobilepauker2.lesson

class LongTermBatch(lastBatchIndex: Int): Batch(mutableListOf()) {
    companion object{
        private val ONE_SECOND: Long = 1000
        private val ONE_MINUTE = ONE_SECOND * 60
        private val ONE_HOUR = ONE_MINUTE * 60
        private val ONE_DAY = ONE_HOUR * 24
        val EXPIRATION_UNIT = ONE_DAY
    }
}