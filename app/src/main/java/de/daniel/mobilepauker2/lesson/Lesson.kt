package de.daniel.mobilepauker2.lesson

class Lesson {
    var description = ""
    var unlearnedBatch = Batch(mutableListOf())
    var shortTermBatch = mutableListOf<Card>()
    var ultraShortTermBatch = mutableListOf<Card>()
    var summaryBatch = SummaryBatch(this)

    private val longTermBatches = mutableListOf<LongTermBatch>()

    fun getLongTermBatchesSize(): Int = longTermBatches.size

    fun addLongTermBatch():LongTermBatch {
        val newBatch = LongTermBatch(longTermBatches.size)
        longTermBatches.add(newBatch)
        return newBatch
    }

    fun getLongTermBatchFromIndex(index: Int): Batch = longTermBatches[index]

    fun refreshExpiration() {
        //longTermBatches.forEach { it.refreshExpiration() }
    }
}