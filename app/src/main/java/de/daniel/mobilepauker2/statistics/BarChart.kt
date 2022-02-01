package de.daniel.mobilepauker2.statistics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
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
        val data = createBarData(chart)

        prepareChartData(chart, data)
        configureChartAppearance(chart)
    }

    private fun createBarData(chart: BarChart): BarData {
        val barDataList = mutableListOf<BarEntry>()
        val statistics = lessonManager.getBatchStatistics()
        var i = 0
        var titel: String
        var abgl: Float
        var ungel: Float
        var gel: Float

        val labels = mutableListOf<String>()

        while (i < statistics.size) {
            when (i) {
                0 -> {
                    titel = requireContext().resources.getString(R.string.sum)
                    abgl = statistics[i].expiredCardsSize.toFloat()
                    ungel = lessonManager.getBatchSize(BatchType.UNLEARNED).toFloat()
                    gel = statistics[0].batchSize - abgl - ungel
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(ungel, gel, abgl))
                    labels.add(titel)
                    barDataList.add(barEntry)
                }
                1 -> {
                    titel = requireContext().resources.getString(R.string.untrained)
                    ungel = statistics[i].batchSize.toFloat()
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(ungel, 0f, 0f))
                    labels.add(titel)
                    barDataList.add(barEntry)
                }
                else -> {
                    titel = requireContext().getString(R.string.stack) + (i - 1)
                    val sum: Int = statistics[i].batchSize
                    abgl = statistics[i].expiredCardsSize.toFloat()
                    gel = sum - abgl
                    val barEntry = BarEntry(i.toFloat(), floatArrayOf(0f, gel, abgl))
                    labels.add(titel)
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
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawGridLinesBehindData(false)
        chart.xAxis.setDrawLimitLinesBehindData(false)
        chart.xAxis.setDrawLabels(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setCenterAxisLabels(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.isGranularityEnabled = true
        chart.xAxis.labelCount = lessonManager.getBatchStatistics().size
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setDrawValueAboveBar(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.isHighlightFullBarEnabled = false
        chart.setScaleEnabled(false)
        chart.setVisibleXRangeMaximum(5f)
        chart.setVisibleXRangeMinimum(5f)

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value) {
                    0f -> "Sum"
                    1f -> "Ungelernt"
                    else -> "Stack ${value.toInt() - 1}"
                }
            }
        }
    }
}