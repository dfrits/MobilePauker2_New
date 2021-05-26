package de.daniel.mobilepauker2.mainmenu

import android.os.Bundle
import android.os.PersistableBundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.db.chart.view.ChartView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.statistics.ChartAdapter
import de.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback
import javax.inject.Inject

class MainMenu : AppCompatActivity(R.layout.main_menu) {
    @Inject
    lateinit var viewModel: MainMenuViewModel

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    private val context = this
    private lateinit var chartView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        setContentView(R.layout.main_menu)

        if (!lessonManager.isLessonSetup()) lessonManager.createNewLesson()

        initButtons()
        initView()
        initChartList()
    }

    private fun initButtons() {
        val hasCardsToLearn = lessonManager.getBatchSize(BatchType.UNLEARNED) != 0
        val hasExpiredCards = lessonManager.getBatchSize(BatchType.EXPIRED) != 0

        findViewById<ImageButton>(R.id.bLearnNewCard)?.let {
            it.isEnabled = hasCardsToLearn
            it.isClickable = hasCardsToLearn
        }
        findViewById<ImageButton>(R.id.tLearnNewCardDesc)?.isEnabled = hasCardsToLearn

        findViewById<ImageButton>(R.id.bRepeatExpiredCards)?.let {
            it.isEnabled = hasExpiredCards
            it.isClickable = hasExpiredCards
        }
        findViewById<ImageButton>(R.id.tRepeatExpiredCardsDesc)?.isEnabled = hasExpiredCards
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

        title = if (lessonManager.isLessonNotNew()) dataManager.getReadableFileName()
        else getString(R.string.app_name)
    }

    private fun initChartList() {
        // Im Thread laufen lassen um MainThread zu entlasten
        val initthread = Thread {
            chartView = findViewById(R.id.chartListView)
            val layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL, false
            )
            chartView.layoutManager = layoutManager
            chartView.overScrollMode = View.OVER_SCROLL_NEVER
            chartView.isScrollContainer = true
            chartView.isNestedScrollingEnabled = true
            runOnUiThread {
                val onClickListener: ChartAdapterCallback = object : ChartAdapterCallback {
                    override fun onClick(position: Int) {
                        //showBatchDetails(position)
                    }
                }
                val adapter = ChartAdapter(context, onClickListener)
                chartView.adapter = adapter
            }
        }
        initthread.run()
    }
}