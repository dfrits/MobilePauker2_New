package de.daniel.mobilepauker2.statistics

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.models.ModelManager
import java.util.*
import javax.inject.Inject

class ChartAdapter(private val context: Context, val callback: ChartAdapterCallback) :
    RecyclerView.Adapter<ChartAdapter.ViewHolder>() {
    private val batchStatistics: List<BatchStatistics>
    private val lessonSize: Int
    private val chartBars: List<ChartBar>

    @Inject
    lateinit var modelManager: ModelManager

    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.chart_bar, parent, false)
        val titel: String
        val abgelaufen: Int
        val ungelernt: Int
        val gelernt: Int
        val chartBarCallback: ChartBar.ChartBarCallback = object : ChartBar.ChartBarCallback {
            override fun onClick() {
                callback.onClick(position)
            }
        }
        val chartBar = ChartBar(view, chartBarCallback)
        when (position) {
            0 -> {
                titel = context.resources.getString(R.string.sum)
                abgelaufen = 0//modelManager.getExpiredCardsSize()
                ungelernt = 0//modelManager.getUnlearnedBatchSize()
                gelernt = lessonSize - abgelaufen - ungelernt
                chartBar.show(
                    context,
                    titel,
                    lessonSize,
                    gelernt,
                    ungelernt,
                    abgelaufen,
                    lessonSize
                )
            }
            1 -> {
                titel = context.resources.getString(R.string.untrained)
                ungelernt = 0//modelManager.getUnlearnedBatchSize()
                chartBar.show(context, titel, ungelernt, -1, ungelernt, -1, lessonSize)
            }
            else -> {
                titel = context.getString(R.string.stack) + (position - 1)
                val sum = batchStatistics[position - 2].batchSize
                abgelaufen = batchStatistics[position - 2].expiredCardsSize
                gelernt = sum - abgelaufen
                chartBar.show(context, titel, sum, gelernt, -1, abgelaufen, lessonSize)
            }
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = batchStatistics.size + 2

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(
        view
    )

    interface ChartAdapterCallback {
        fun onClick(position: Int)
    }

    init {
        (context as PaukerApplication).appSingletonComponent.inject(this)
        batchStatistics = emptyList()//modelManager.getBatchStatistics()
        lessonSize = 0//modelManager.getLessonSize()
        chartBars = ArrayList(itemCount)
    }
}