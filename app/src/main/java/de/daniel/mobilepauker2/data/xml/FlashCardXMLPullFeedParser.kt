package de.daniel.mobilepauker2.data.xml

import android.util.Xml
import de.daniel.mobilepauker2.lesson.ComponentOrientation
import de.daniel.mobilepauker2.lesson.FlashCard
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.util.*

class FlashCardXMLPullFeedParser(feedUrl: URL) : FlashCardBasedFeedParser(feedUrl) {

    override fun parse(): Lesson {
        var flashCards: MutableList<FlashCard>? = null
        val parser = Xml.newPullParser()
        var batchCount = 0
        var description: String? = "No Description"
        return try {
            parser.setInput(getInputStream(), null)
            var eventType = parser.eventType
            var currentFlashCard: FlashCard? = null
            var SIDEA = false
            var SIDEB = false
            var done = false
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                var name: String
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> flashCards = ArrayList<FlashCard>()
                    XmlPullParser.START_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            val lessonFormatString = parser.getAttributeValue(null, "LessonFormat")
                            if (lessonFormatString != null) {
                                val lessonFormat = lessonFormatString.toFloat()
                                Log.d(
                                    "FlashCardXMLPullFeedParser::parse",
                                    "Lesson format is $lessonFormat"
                                )
                            }
                        } else if (name.equals(
                                DESCRIPTION,
                                ignoreCase = true
                            )
                        ) {
                            description = parser.nextText()
                            if (description == null) {
                                description = "No description"
                            }
                        } else if (name.equals(
                                CARD,
                                ignoreCase = true
                            )
                        ) {
                            currentFlashCard = FlashCard()
                        } else if (name.equals(
                                BATCH,
                                ignoreCase = true
                            )
                        ) {
                            batchCount++
                        } else if (currentFlashCard != null) {
                            currentFlashCard.initialBatch = batchCount - 1
                            if (name.equals(
                                    FRONTSIDE,
                                    ignoreCase = true
                                ) || name.equals(
                                    REVERSESIDE,
                                    ignoreCase = true
                                )
                            ) {
                                var orientation = parser.getAttributeValue(null, "Orientation")
                                orientation = orientation ?: Constants.STANDARD_ORIENTATION
                                val repeatByTyping =
                                    parser.getAttributeValue(null, "RepeatByTyping")
                                val bRepeatByTyping =
                                    if (repeatByTyping == null) Constants.STANDARD_REPEAT else repeatByTyping == "true"
                                val learnedTimestamp =
                                    parser.getAttributeValue(null, "LearnedTimestamp")
                                if (name.equals(
                                        FRONTSIDE,
                                        ignoreCase = true
                                    )
                                ) {
                                    SIDEA = true
                                    SIDEB = false
                                    currentFlashCard.frontSide.orientation =
                                        ComponentOrientation(orientation)
                                    currentFlashCard.setRepeatByTyping(bRepeatByTyping)

                                    if (learnedTimestamp != null) {
                                        val l = learnedTimestamp.trim { it <= ' ' }.toLong()
                                        currentFlashCard.setLearnedTimeStamp(l)
                                    }
                                } else if (name.equals(
                                        REVERSESIDE,
                                        ignoreCase = true
                                    )
                                ) {
                                    SIDEA = false
                                    SIDEB = true
                                    currentFlashCard.reverseSide.orientation =
                                        ComponentOrientation(orientation)
                                    currentFlashCard.reverseSide.setRepeatByTyping(bRepeatByTyping)

                                    if (learnedTimestamp != null) {
                                        val l = learnedTimestamp.trim { it <= ' ' }.toLong()
                                        currentFlashCard.reverseSide.setLearnedTimeStamp(l)
                                    }
                                }

                                //Log.d("FlashCardXMLPullFeedParser::parse", "orientation=" + orientation);
                            } else if (name.equals(
                                    TEXT,
                                    ignoreCase = true
                                )
                            ) {
                                if (SIDEA) {
                                    currentFlashCard.sideAText = parser.nextText()
                                    //Log.d("FlashCardXMLPullFeedParser::parse","sideA=" + currentFlashCard.getSideAText());
                                } else if (SIDEB) {
                                    currentFlashCard.sideBText = parser.nextText()
                                } else {
                                    currentFlashCard.sideAText = "Empty"
                                    currentFlashCard.sideBText = "Empty"
                                }
                            } else if (name.equals(
                                    FONT,
                                    ignoreCase = true
                                )
                            ) {
                                var background = parser.getAttributeValue(null, "Background")
                                var bold = parser.getAttributeValue(null, "Bold")
                                var family = parser.getAttributeValue(null, "Family")
                                var foreground = parser.getAttributeValue(null, "Foreground")
                                var italic = parser.getAttributeValue(null, "Italic")
                                var size = parser.getAttributeValue(null, "Size")

                                // Set to defaults if null
                                if (background == null) {
                                    background = "-1"
                                }
                                if (bold == null) {
                                    bold = "false"
                                }
                                if (family == null) {
                                    family = "Dialog"
                                }
                                if (foreground == null) {
                                    foreground = "-16777216"
                                }
                                if (italic == null) {
                                    italic = "false"
                                }
                                if (size == null) {
                                    size = "12"
                                }
                                if (SIDEA) {
                                    currentFlashCard.frontSide.font =
                                        Font(
                                            background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size
                                        )
                                } else if (SIDEB) {
                                    currentFlashCard.reverseSide.font =
                                        Font(
                                            background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size
                                        )
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        name = parser.name
                        if (name.equals(CARD, ignoreCase = true) && currentFlashCard != null) {
                            flashCards?.add(currentFlashCard)
                        } else if (name.equals(LESSON, ignoreCase = true)) {
                            done = true
                        }
                    }
                }
                eventType = parser.next()
            }

            if (flashCards != null) {
                setupLesson(flashCards, description)
            } else {
                Lesson()
            }
        } catch (e: Exception) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.message, e)
            throw RuntimeException(e)
        }
    }

    private fun setupLesson(flashCardList: List<FlashCard>, description: String?): Lesson {
        val newLesson = Lesson()
        /*val summaryBatch: Batch = newLesson.getSummaryBatch()
        newLesson.setDescription(description)
        for (i in flashCardList.indices) {
            val flashCard: FlashCard = flashCardList[i]
            if (flashCard.getInitialBatch() < 3) {
                flashCard.setLearned(false)
            } else {
                flashCard.getFrontSide()
                    .setLearned(true) // Warning using flash card set learned here sets the learned timestamp!
            }
            if (newLesson.getNumberOfLongTermBatches() < flashCard.getInitialBatch() - 2) {
                Log.d(
                    "FC~XMLPullFeedParser::setupLesson",
                    "num of long term batches=" + newLesson.getNumberOfLongTermBatches()
                )
                Log.d(
                    "FC~XMLPullFeedParser::setupLesson",
                    "card initla batch=" + flashCard.getInitialBatch()
                )
                val batchesToAdd: Int =
                    flashCard.getInitialBatch() - 2 - newLesson.getNumberOfLongTermBatches()
                Log.d("FC~XMLPullFeedParser::setupLesson", "batchsToAdd$batchesToAdd")
                for (j in 0 until batchesToAdd) {
                    newLesson.addLongTermBatch()
                }
            }
            var batch: Batch
            if (flashCard.isLearned()) {
                // must put the card into the corresponding long
                // term batch
                batch = newLesson.getLongTermBatch(
                    flashCard.getInitialBatch() - 3
                )
            } else {
                // must put the card into the unlearned batch
                batch = newLesson.getUnlearnedBatch()
            }
            batch.addCard(flashCard)
            summaryBatch.addCard(flashCard)
        }

        newLesson.refreshExpiration()
        printLessonToDebug(newLesson)*/
        return newLesson
    }

    /**
     * Findet das nächste Ablaufdatum. Falls keines gefunden wird, wird [Long.MIN_VALUE]
     * zurückgegeben.
     * @return Eine Map mit dem frühesten Ablaufdatum **(index = 0)** und die Anzahl abgelaufener
     * Karten (**index = 1)**
     */
    /*fun getNextExpireDate(): SparseLongArray {
        val parser = Xml.newPullParser()
        val map = SparseLongArray(2)
        val currentTimestamp = System.currentTimeMillis()
        return try {
            // auto-detect the encoding from the stream
            parser.setInput(getInputStream(), null)
            var eventType = parser.eventType
            var nextExpireTimeStamp = Long.MIN_VALUE
            var batchCount = 0
            var expiredCards: Long = 0
            var done = false
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                var name: String
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            val lessonFormatString = parser.getAttributeValue(null, "LessonFormat")
                            if (lessonFormatString != null) {
                                val lessonFormat = lessonFormatString.toFloat()
                                Log.d(
                                    "FlashCardXMLPullFeedParser::parse",
                                    "Lesson format is $lessonFormat"
                                )
                            }
                        } else if (name.equals(
                                BATCH,
                                ignoreCase = true
                            )
                        ) {
                            batchCount++
                        } else if (name.equals(
                                FRONTSIDE,
                                ignoreCase = true
                            )
                            || name.equals(
                                REVERSESIDE,
                                ignoreCase = true
                            )
                        ) {
                            val learnedTimestamp =
                                parser.getAttributeValue(null, "LearnedTimestamp")
                            if (learnedTimestamp != null) {
                                val factor = Math.E.pow((batchCount - 4).toDouble())
                                val expirationTime =
                                    (LongTermBatch.getExpirationUnit() * factor) as Long
                                try {
                                    val expireTimeStamp = learnedTimestamp.toLong() + expirationTime
                                    if (nextExpireTimeStamp == Long.MIN_VALUE
                                        || expireTimeStamp < nextExpireTimeStamp
                                    ) {
                                        nextExpireTimeStamp = expireTimeStamp
                                    }
                                    val diff = currentTimestamp - learnedTimestamp.toLong()
                                    if (diff > expirationTime) {
                                        expiredCards++
                                    }
                                } catch (ignored: NumberFormatException) {
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            done = true
                        }
                    }
                }
                eventType = parser.next()
            }
            map.put(0, nextExpireTimeStamp)
            map.put(1, expiredCards)
            map
        } catch (e: Exception) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.message, e)
            throw RuntimeException(e)
        }
    }*/
}