package com.code4you.geodumb

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.code4you.geodumb.api.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class StatsDetailActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadStats()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMySegnalazioni()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val publishedCount = list.size

                    // Contatori principali
                    findViewById<TextView>(R.id.tv_detail_sent).text = publishedCount.toString()
                    findViewById<TextView>(R.id.tv_detail_published).text = publishedCount.toString()
                    // "Nuove nel quartiere" – da implementare con la tua logica
                    findViewById<TextView>(R.id.tv_detail_new).text = "0"

                    if (publishedCount == 0) {
                        findViewById<TextView>(R.id.tv_empty_state).visibility = View.VISIBLE
                        return@launch
                    }

                    // Ultima segnalazione
                    val last = list.maxByOrNull { it.imageTime ?: "" }
                    if (last != null) {
                        val dateStr = last.imageTime?.substringBefore("T") ?: "-"
                        findViewById<TextView>(R.id.tv_last_report_date).text = dateStr
                        try {
                            val lastDate = LocalDateTime.parse(last.imageTime?.substringBefore("."))
                            val days = ChronoUnit.DAYS.between(lastDate, LocalDateTime.now())
                            findViewById<TextView>(R.id.tv_last_report_days).text = "$days giorni fa"
                        } catch (e: Exception) {
                            findViewById<TextView>(R.id.tv_last_report_days).text = "-"
                        }
                    }

                    // Statistiche temporali
                    val now = LocalDateTime.now()
                    val thisMonthCount = list.count {
                        try {
                            LocalDateTime.parse(it.imageTime?.substringBefore(".")).month == now.month
                        } catch (e: Exception) { false }
                    }
                    findViewById<TextView>(R.id.tv_this_month).text = thisMonthCount.toString()

                    val firstDate = try {
                        LocalDateTime.parse(list.first().imageTime?.substringBefore("."))
                    } catch (e: Exception) { now }
                    val weeks = ChronoUnit.WEEKS.between(firstDate, now).coerceAtLeast(1)
                    val avg = String.format("%.1f", list.size.toFloat() / weeks)
                    findViewById<TextView>(R.id.tv_weekly_avg).text = avg

                    // Distribuzione per tipologia
                    val typeCount = list.groupingBy { it.typo ?: "Altro" }.eachCount()
                    val typesContainer = findViewById<LinearLayout>(R.id.types_container)
                    typesContainer.removeAllViews()
                    for ((type, count) in typeCount) {
                        val item = layoutInflater.inflate(R.layout.item_stat_type_bar, typesContainer, false)
                        item.findViewById<TextView>(R.id.tv_type_name).text = type
                        item.findViewById<TextView>(R.id.tv_type_count).text = count.toString()
                        val bar = item.findViewById<View>(R.id.view_bar)
                        val maxWidth = (typesContainer.width - 32).coerceAtLeast(200)
                        val percent = count.toFloat() / list.size
                        bar.layoutParams = bar.layoutParams.apply {
                            width = (maxWidth * percent).toInt()
                        }
                        typesContainer.addView(item)
                    }
                } else {
                    Toast.makeText(this@StatsDetailActivity, "Errore nel caricamento", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StatsDetailActivity, "Errore di connessione", Toast.LENGTH_SHORT).show()
            }
        }
    }
}