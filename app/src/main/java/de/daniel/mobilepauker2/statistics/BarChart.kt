package de.daniel.mobilepauker2.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import javax.inject.Inject

class BarChart : Fragment(R.layout.chart_bar) {

    @Inject
    lateinit var lessonManager: LessonManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity().applicationContext as PaukerApplication)
            .applicationSingletonComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chart = view.findViewById<BarChart>(R.id.chartBar)
        val data = createBarData()

        prepareChartData(chart, data)
        configureChartAppearance(chart)
    }

    private fun createBarData(): BarData {
        val barDataList = mutableListOf<BarEntry>()
        val statistics = lessonManager.getBatchStatistics()
        val lessonSize = lessonManager.getBatchSize(BatchType.LESSON)
        var i = 0
        var titel: String
        var abgl: Float
        var ungel: Float
        var gel: Float

        while (i < statistics.size) {
            when (i) {
                0 -> {
                    titel = requireContext().resources.getString(R.string.sum)
                    abgl = lessonManager.getBatchSize(BatchType.EXPIRED).toFloat()
                    ungel = lessonManager.getBatchSize(BatchType.UNLEARNED).toFloat()
                    gel = lessonSize - abgl - ungel
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(ungel, gel, abgl))
                    barDataList.add(barEntry)
                }
                1 -> {
                    titel = requireContext().resources.getString(R.string.untrained)
                    ungel = lessonManager.getBatchSize(BatchType.UNLEARNED).toFloat()
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(ungel, 0f, 0f))
                    barDataList.add(barEntry)
                }
                else -> {
                    titel = requireContext().getString(R.string.stack) + (i - 1)
                    val sum: Int = statistics[i - 2].batchSize
                    abgl = statistics[i - 2].expiredCardsSize.toFloat()
                    gel = sum - abgl
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(0f, gel, abgl))
                    barDataList.add(barEntry)
                }
            }
            i++
        }

        val set1 = BarDataSet(barDataList, "LessonStatistic")
        set1.setDrawIcons(false)
        set1.setDrawValues(false)
        set1.setColors(
            resources.getColor(R.color.unlearned, null),
            resources.getColor(R.color.learned, null),
            resources.getColor(R.color.expired, null)
        )

        val barData = BarData(arrayListOf<IBarDataSet>(set1))
        // TODO setFormatter
        return barData
    }

    private fun prepareChartData(chart: BarChart, data: BarData) {
        chart.data = data
        chart.invalidate()
    }

    private fun configureChartAppearance(chart: BarChart) {
        chart.setDrawBorders(false)
        chart.axisLeft.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setDrawValueAboveBar(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.isHighlightFullBarEnabled = false
        chart.xAxis.setCenterAxisLabels(true)
        chart.setScaleEnabled(false)
        chart.setVisibleXRangeMaximum(5f)
    }
}