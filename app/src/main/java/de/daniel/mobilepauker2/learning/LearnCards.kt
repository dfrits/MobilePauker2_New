package de.daniel.mobilepauker2.learning

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.danilomendes.progressbar.InvertedTextProgressbar
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.learning.TimerService.*
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lesson.card.CardPackAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCard.SideShowing.*
import de.daniel.mobilepauker2.models.LearningPhase
import de.daniel.mobilepauker2.models.LearningPhase.*
import de.daniel.mobilepauker2.models.LearningPhase.Companion.currentPhase
import de.daniel.mobilepauker2.models.LearningPhase.Companion.setLearningPhase
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.*
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.ErrorReporter
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class LearnCards : FlashCardSwipeScreen() {
    private val context: Context = this
    private var pendingIntent: Intent? = null
    private var notificationManager: NotificationManagerCompat? = null
    private val flipCardSides = false
    private val completedLearning = false
    private var repeatingLTM = false
    private var stopWaiting = false
    private var firstStart = true
    private var timerServiceConnection: ServiceConnection? = null
    private lateinit var timerService: TimerService
    private lateinit var timerServiceIntent: Intent
    private lateinit var ustmTimerBar: InvertedTextProgressbar
    private lateinit var stmTimerBar: InvertedTextProgressbar
    private val bNext: Button? = null
    private val bShowMe: Button? = null
    private val lRepeatButtons: RelativeLayout? = null
    private val lSkipWaiting: RelativeLayout? = null
    private var pauseButton: MenuItem? = null
    private var restartButton: MenuItem? = null
    private var timerAnimation: RelativeLayout? = null
    private val ustmTimerText: String? = null

    @Inject
    lateinit var viewModel: LearnCardsViewModel

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        @Suppress("Deprecation")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (currentPhase !== REPEATING_LTM
            && (currentPhase !== SIMPLE_LEARNING
                || currentPhase !== NOTHING)) {
            // A check on mActivitySetupOk is done here as onCreate is called even if the
            // super (FlashCardSwipeScreenActivity) onCreate fails to find any cards and calls finish()
            if (mActivitySetupOk) {
                initTimer()
            }
        } else if (currentPhase === REPEATING_LTM) {
            repeatingLTM = true
        }
    }

    override fun updateCurrentCard() {
        try {
            if (isCardCursorAvailable()) {
                currentCard.sideAText = mCardCursor.getString(CardPackAdapter.KEY_SIDEA_ID)
                currentCard.sideBText = mCardCursor.getString(CardPackAdapter.KEY_SIDEB_ID)
                val learnStatus = mCardCursor.getString(CardPackAdapter.KEY_LEARN_STATUS_ID)
                currentCard.isLearned = learnStatus!!.contentEquals("1")
            } else {
                currentCard.sideAText = ""
                currentCard.sideBText = ""
                Log.d("FlashCardSwipeScreenActivity::updateCurrentCard", "Card Cursor not available")
            }
        } catch (e: Exception) {
            Log.e("FlashCardSwipeScreenActivity::updateCurrentCard", "Caught Exception")
            toaster.showToast(context as Activity, R.string.load_card_data_error, Toast.LENGTH_SHORT)
            errorReporter.addCustomData("LearnCardsActivity::updateCurrentCard", "cursor problem?")
            finish()
        }
    }

    override fun screenTouched() {
        if (timerService.isUstmTimerPaused() || timerService.isStmTimerPaused()) return

        val learningPhase: LearningPhase = currentPhase

        if (learningPhase == REPEATING_LTM || learningPhase == REPEATING_STM || learningPhase == REPEATING_USTM) {
            if (lessonManager.getCardFromCurrentPack(mCardCursor.position)!!.isRepeatedByTyping) {
                showInputDialog()
            } else {
                if (flipCardSides) {
                    currentCard.side = SIDE_A
                } else {
                    currentCard.side = SIDE_B
                }
                fillInData(flipCardSides)
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
    }

    override fun fillData() {
        // Prüfen, ob getauscht werden soll
        val flipCardSides: Boolean = hasCardsToBeFlipped()
        fillInData(flipCardSides)
    }

    override fun cursorLoaded() {
        Log.d("LearnCardsActivity::cursorLoaded", "cursor loaded: " +
            "savedPos= " + mSavedCursorPosition)
        if (mSavedCursorPosition == -1) {
            setCursorToFirst()
            updateCurrentCard()
            fillData()
            setButtonsVisibility()
        } else {
            mCardCursor.moveToPosition(mSavedCursorPosition)
            updateCurrentCard()
            fillInData(flipCardSides)
            if (bShowMe!!.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SIDE_A
                    || !flipCardSides && currentCard.side == SIDE_B)) {
                bShowMe.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
        mSavedCursorPosition = -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            updateCurrentCard()
            fillInData(flipCardSides)
            setButtonsVisibility()
            if (bShowMe!!.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SIDE_A
                    || !flipCardSides && currentCard.side == SIDE_B)) {
                bShowMe.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                toaster.showToast(context as Activity, R.string.saving_success, Toast.LENGTH_SHORT)
                dataManager.saveRequired = false
                toaster.showExpireToast(context)
            } else {
                toaster.showToast(context as Activity, R.string.saving_error, Toast.LENGTH_SHORT)
            }
            finish()
        }
        pendingIntent = null

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.exit_learning_dialog)
            .setPositiveButton(R.string.yes) { _, _ ->
                timerService.stopStmTimer()
                timerService.stopUstmTimer()
                Log.d("LearnCardsActivity::onBackPressed", "Finish and Timer stopped")
                finish()
            }
            .setNeutralButton(R.string.cancel) { dialog, which -> dialog.cancel() }
            .create().show()
    }

    override fun onPause() {
        super.onPause()
        mSavedCursorPosition = try {
            mCardCursor.position
        } catch (e: java.lang.Exception) {
            -1
        }
    }

    override fun onResume() {
        super.onResume()
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(context)
        }
        notificationManager?.cancelAll()
        if (!firstStart && !restartButton!!.isVisible) {
            restartTimer()
            if (mSavedCursorPosition != -1) {
                refreshCursor()
            }
        }

        if (currentPhase === WAITING_FOR_USTM || currentPhase === WAITING_FOR_STM) {
            showHideTimerAnimation()
        }
        firstStart = false
    }

    override fun onDestroy() {
        if (timerServiceConnection != null) {
            stopService(timerServiceIntent)
            unbindService(timerServiceConnection!!)
        }
        notificationManager?.cancelAll()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.learning_cards, menu)
        pauseButton = menu.findItem(R.id.mPauseButton)
        restartButton = menu.findItem(R.id.mRestartButton)
        if (currentPhase === REPEATING_LTM || currentPhase === SIMPLE_LEARNING
            || currentPhase === NOTHING || currentPhase === REPEATING_STM) {
            pauseButton?.isVisible = false
            restartButton?.isVisible = false
        }
        return true
    }

    private fun initTimer() {
        val ustmTotalTime: Int = settingsManager.getStringPreference(USTM)?.toInt() ?: 0
        ustmTimerBar = findViewById(R.id.UKZGTimerBar)
        ustmTimerBar.maxProgress = ustmTotalTime
        ustmTimerBar.minProgress = 0
        val stmTotalTime: Int = settingsManager.getStringPreference(STM)?.toInt() ?: 0
        stmTimerBar = findViewById(R.id.KZGTimerBar)
        stmTimerBar.maxProgress = stmTotalTime * 60
        stmTimerBar.minProgress = 0
        timerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d("LearnActivity::initTimer", "onServiceConnectedCalled")
                val binder: LocalBinder = service as LocalBinder
                timerService = binder.serviceInstance
                registerListener()
                timerService.startUstmTimer()
                timerService.startStmTimer()
                findViewById<RelativeLayout>(R.id.lTimerFrame).setVisibility(View.VISIBLE)
                timerAnimation = findViewById(R.id.timerAnimationPanel)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d("LearnActivity::initTimer", "onServiceDisconnectedCalled")
                timerService.stopUstmTimer()
                timerService.stopStmTimer()
            }

            override fun onBindingDied(name: ComponentName) {}
        }
        timerServiceIntent = Intent(context, TimerService::class.java)
        timerServiceIntent.putExtra(TimerService.USTM_TOTAL_TIME, ustmTotalTime)
        timerServiceIntent.putExtra(TimerService.STM_TOTAL_TIME, stmTotalTime)
        startService(timerServiceIntent)
        bindService(timerServiceIntent, timerServiceConnection!!, BIND_AUTO_CREATE)
    }

    private fun registerListener() {
        /*registerReceiver(ustmTimeBroadcastReceiver, IntentFilter(TimerService.ustm_receiver))
        registerReceiver(stmTimeBroadcastReceiver, IntentFilter(TimerService.stm_receiver))
        registerReceiver(ustmFinishedBroadcastReceiver, IntentFilter(TimerService.ustm_finished_receiver))
        registerReceiver(stmFinishedBroadcastReceiver, IntentFilter(TimerService.stm_finished_receiver))*/
    }

    private fun pauseTimer() {
        timerService.pauseTimers()
        if (!timerService.isStmTimerFinished()) {
            disableButtons()
        }
    }

    private fun restartTimer() {
        timerService.restartTimers()
        enableButtons()
    }

    private fun updateLearningPhase() {
        var zeroUnlearnedCards = false
        var zeroUSTMCards = false
        var zeroSTMCards = false
        if (lessonManager.getBatchSize(BatchType.UNLEARNED) <= 0) {
            zeroUnlearnedCards = true
        }
        if (lessonManager.getBatchSize(BatchType.ULTRA_SHORT_TERM) <= 0) {
            zeroUSTMCards = true
        }
        if (lessonManager.getBatchSize(BatchType.SHORT_TERM) <= 0) {
            zeroSTMCards = true
        }
        when (currentPhase) {
            NOTHING -> {}
            SIMPLE_LEARNING -> {
                if (completedLearning) {
                    finishLearning()
                } else {
                    setButtonVisibilityRepeating()
                }
            }
            FILLING_USTM -> {
                setButtonVisibilityFilling()
                if (timerService.isStmTimerFinished()) // STM timeout so go straight to repeating ustm cards
                {
                    setLearningPhase(REPEATING_USTM)
                    updateLearningPhase()
                } else if (zeroUnlearnedCards && !timerService.isUstmTimerFinished()) {
                    setLearningPhase(WAITING_FOR_USTM)
                    updateLearningPhase()
                } else if (timerService.isUstmTimerFinished()) {
                    setLearningPhase(REPEATING_USTM)
                    updateLearningPhase()
                }
            }
            WAITING_FOR_USTM -> {
                Log.d("LearnCardsActivity::updateLearningPhase", "Waiting for USTM")
                // Gif zeigen
                showHideTimerAnimation()

                // USTM Timeout
                if (timerService.isUstmTimerFinished() || stopWaiting) {
                    stopWaiting = false
                    setLearningPhase(REPEATING_USTM)
                    timerService.stopUstmTimer()
                    updateLearningPhase()
                }
            }
            REPEATING_USTM -> {
                setButtonsVisibility()
                if (zeroUSTMCards) // We have learned all the ustm cards
                {
                    if (timerService.isStmTimerFinished()) //STM timer has timed out so move to repeating STM
                    {
                        setLearningPhase(REPEATING_STM)
                    } else if (!zeroUnlearnedCards) // Unlearned cards available so go back to filling ustm;
                    {
                        setLearningPhase(FILLING_USTM)
                        timerService.startUstmTimer()
                    } else {
                        setLearningPhase(WAITING_FOR_STM)
                    }
                    updateLearningPhase()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(REPEATING_USTM)
                }
            }
            WAITING_FOR_STM -> {

                // Gif zeigen
                showHideTimerAnimation()

                // USTM Timeout
                if (timerService.isStmTimerFinished() || stopWaiting) {
                    stopWaiting = false
                    timerService.stopStmTimer()
                    setLearningPhase(REPEATING_STM)
                    invalidateOptionsMenu()
                    updateLearningPhase()
                }
            }
            REPEATING_STM -> {
                setButtonsVisibility()
                if (zeroSTMCards) {
                    finishLearning()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(REPEATING_STM)
                }
            }
            REPEATING_LTM -> {
                if (completedLearning && lessonManager.getBatchSize(BatchType.EXPIRED) <= 0) {
                    finishLearning()
                } else if (completedLearning) {
                    pushCursorToNext()
                } else {
                    setButtonsVisibility()
                }
            }
            else -> {}
        }
    }

    fun showInputDialog() {

    }

    fun showHideTimerAnimation() {

    }

    fun finishLearning() {

    }

    fun pushCursorToNext() {

    }

    fun setButtonsVisibility() {

    }

    fun setButtonVisibilityFilling() {

    }

    fun setButtonVisibilityRepeating() {

    }

    fun enableButtons() {

    }

    fun disableButtons() {

    }

    fun fillInData(flipCardSides: Boolean) {

    }

    fun hasCardsToBeFlipped(): Boolean {
        return false
    }
}