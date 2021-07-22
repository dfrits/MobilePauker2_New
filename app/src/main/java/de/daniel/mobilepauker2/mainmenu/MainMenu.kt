package de.daniel.mobilepauker2.mainmenu

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lessonimport.LessonImport
import de.daniel.mobilepauker2.statistics.ChartAdapter
import de.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class MainMenu : AppCompatActivity(R.layout.main_menu) {
    @Inject
    lateinit var viewModel: MainMenuViewModel

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var toaster: Toaster

    private val context = this
    private val RQ_WRITE_EXT_SAVE = 98
    private val RQ_WRITE_EXT_OPEN = 99
    private var chartView: RecyclerView? = null
    private var firstStart = true
    private lateinit var search: MenuItem

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        setContentView(R.layout.main_menu)

        if (!lessonManager.isLessonSetup()) lessonManager.createNewLesson()

        initButtons()
        initView()
        initChartList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val save = menu.findItem(R.id.mSaveFile)
        search = menu.findItem(R.id.mSearch)
        val open = menu.findItem(R.id.mOpenLesson)
        menu.setGroupEnabled(
            R.id.mGroup,
            lessonManager.isLessonNotNew() || !lessonManager.isLessonEmpty()
        )
        if (lessonManager.getBatchSize(BatchType.LESSON) > 0) {
            search.isVisible = true
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        } else {
            search.isVisible = false
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        save.isVisible = dataManager.saveRequired
        if (search.isVisible) {
            val searchView = search.actionView as SearchView
            searchView.isIconifiedByDefault = false
            searchView.isIconified = false
            searchView.queryHint = getString(R.string.search_hint)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {

                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
            searchView.setOnQueryTextFocusChangeListener { v, hasFocus -> if (!hasFocus) searchView.clearFocus() }
        }
        return true
    }

    override fun onPause() {
        chartView = null
        super.onPause()
    }

    override fun onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY")
        super.onResume()
        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        lessonManager.resetLesson()
        //search.collapseActionView() // TODO
        if (!firstStart) {
            initButtons()
            initView()
            initChartList()
            invalidateOptionsMenu()
        }
        firstStart = false
        NotificationManagerCompat.from(context).cancelAll()
    }

    override fun onBackPressed() {
        if (dataManager.saveRequired) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.close_without_saving_dialog_msg)
                .setPositiveButton(R.string.cancel, null)
                .setNeutralButton(R.string.close) { _, _ -> finish() }
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.unlearned))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.learned))
        } else super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == -1) return
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putBoolean("FirstTime", false).apply()

        if (requestCode == RQ_WRITE_EXT_OPEN && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openLesson()
        }
        if (requestCode == RQ_WRITE_EXT_SAVE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveLesson(REQUEST_CODE_SAVE_DIALOG_NORMAL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                toaster.showToast(R.string.saving_success, Toast.LENGTH_SHORT)
                dataManager.saveRequired = false

                toaster.showExpireToast(context)
            }
            invalidateOptionsMenu()
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON && resultCode == RESULT_OK) {
            createNewLesson()
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_OPEN && resultCode == RESULT_OK) {
            startActivity(Intent(context, LessonImport::class.java))
        }
    }

    private fun initButtons() {
        val hasCardsToLearn = lessonManager.getBatchSize(BatchType.UNLEARNED) != 0
        val hasExpiredCards = lessonManager.getBatchSize(BatchType.EXPIRED) != 0

        findViewById<ImageButton>(R.id.bLearnNewCard)?.let {
            it.isEnabled = hasCardsToLearn
            it.isClickable = hasCardsToLearn
        }
        findViewById<TextView>(R.id.tLearnNewCardDesc)?.isEnabled = hasCardsToLearn

        findViewById<ImageButton>(R.id.bRepeatExpiredCards)?.let {
            it.isEnabled = hasExpiredCards
            it.isClickable = hasExpiredCards
        }
        findViewById<TextView>(R.id.tRepeatExpiredCardsDesc)?.isEnabled = hasExpiredCards
    }

    private fun initView() {
        invalidateOptionsMenu()

        val description: String = lessonManager.lessonDescription
        val descriptionView: TextView = findViewById(R.id.infoText)
        descriptionView.text = description
        if (description.isNotEmpty()) {
            descriptionView.movementMethod = ScrollingMovementMethod()
        }

        findViewById<SlidingUpPanelLayout>(R.id.drawerPanel)?.let {
            it.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(panel: View, slideOffset: Float) {}
                override fun onPanelStateChanged(
                    panel: View,
                    previousState: PanelState,
                    newState: PanelState
                ) {
                    if (newState == PanelState.EXPANDED)
                        findViewById<ImageView>(R.id.drawerImage).rotation = 180f
                    if (newState == PanelState.COLLAPSED)
                        findViewById<ImageView>(R.id.drawerImage).rotation = 0f
                }
            })
            it.panelState = PanelState.COLLAPSED
        }

        title = if (lessonManager.isLessonNotNew()) dataManager.getReadableCurrentFileName()
        else getString(R.string.app_name)
    }

    private fun initChartList() {
        // Im Thread laufen lassen um MainThread zu entlasten
        Thread {
            chartView = findViewById(R.id.chartListView)
            val layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL, false
            )
            chartView?.let {
                it.layoutManager = layoutManager
                it.overScrollMode = View.OVER_SCROLL_NEVER
                it.isScrollContainer = true
                it.isNestedScrollingEnabled = true
                runOnUiThread {
                    val onClickListener: ChartAdapterCallback = object : ChartAdapterCallback {
                        override fun onClick(position: Int) {
                            //showBatchDetails(position) // TODO
                        }
                    }
                    val adapter = ChartAdapter(application as PaukerApplication, onClickListener)
                    it.adapter = adapter
                }
            }
        }.run()
    }

    private fun openLesson() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission(RQ_WRITE_EXT_OPEN)
        } else {
            if (dataManager.saveRequired) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(R.string.lesson_not_saved_dialog_title)
                    .setMessage(R.string.save_lesson_before_question)
                    .setPositiveButton(R.string.save) { _, _ ->
                        saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_OPEN)
                    }
                    .setNeutralButton(R.string.open_lesson) { dialog, _ ->
                        startActivity(Intent(context, LessonImport::class.java))
                        dialog.dismiss()
                    }
                builder.create().show()
            } else startActivity(Intent(context, LessonImport::class.java))
        }
    }

    private fun saveLesson(requestCode: Int) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission(RQ_WRITE_EXT_SAVE)
        } //else startActivityForResult(Intent(context, SaveDialog::class.java), requestCode) TODO
    }

    private fun checkPermission(requestCode: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.app_name).setPositiveButton(R.string.next) { dialog, _ ->
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
            dialog.dismiss()
        }.setNeutralButton(R.string.not_now) { dialog, _ -> dialog.dismiss() }

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            builder.setMessage(R.string.write_permission_rational_message)
        } else {
            if (pref.getBoolean("FirstTime", true)) {
                builder.setMessage(R.string.write_permission_info_message)
            } else {
                builder.setMessage(R.string.write_permission_rational_message)
                    .setPositiveButton(R.string.settings) { dialog, _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                        dialog.dismiss()
                    }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun createNewLesson() {
        viewModel.createNewLesson()
        initButtons()
        initChartList()
        initView()
    }

    // Menu clicks
    fun mSaveFileClicked(menuItem: MenuItem) {

    }

    fun mOpenSearchClicked(menuItem: MenuItem) {

    }

    fun mOpenLessonClicked(menuItem: MenuItem) {
        openLesson()
    }

    fun mNewLessonClicked(menuItem: MenuItem) {

    }

    fun mResetLessonClicked(menuItem: MenuItem) {

    }

    fun mFlipSidesClicked(menuItem: MenuItem) {

    }

    fun mEditInfoTextClicked(menuItem: MenuItem) {

    }

    fun mSettingsClicked(menuItem: MenuItem) {

    }

    // Button clicks

    fun addNewCard(view: View) {

    }

    fun learnNewCard(view: View) {

    }

    fun repeatCards(view: View) {

    }
}