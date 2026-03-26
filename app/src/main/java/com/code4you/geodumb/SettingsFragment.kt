package com.code4you.geodumb

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onResume() {
        super.onResume()
        updateReportCounter()
        updateReportUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchCity = view.findViewById<Switch>(R.id.switch_city_filter)
        val textCity = view.findViewById<TextView>(R.id.text_city)
        //val editCity = view.findViewById<EditText>(R.id.text_city)

        switchCity.setOnCheckedChangeListener { _, isChecked ->
            textCity.alpha = if (isChecked) 1.0f else 0.4f
            //editCity.isEnabled = isChecked
        }

        val spinnerTheme = view.findViewById<Spinner>(R.id.spinner_theme)

        // 🔹 Temi esempio
        val themes = listOf("Chiaro", "Scuro", "Sistema")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            themes
        )

        spinnerTheme.adapter = adapter
    }

    // ✅ INCREMENTO CONTATORE
    private fun updateReportCounter() {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)

        val sent = prefs.getInt("reports_sent", 0)
        // Prefernces helper for limit report count
        val limit = PrefsManager.getReportLimit(requireContext())
        //val limit = prefs.getInt("reports_limit", 1)

        val text = view?.findViewById<TextView>(R.id.text_report_limit)

        text?.text = if (sent >= limit) {
            "Limite raggiunto ($sent / $limit)"
        } else {
            "Segnalazioni: $sent / $limit"
        }
        //text?.text = "Segnalazioni: $sent / $limit"
    }

    // ✅ AGGIORNA LA UI
    private fun updateReportUI() {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        val sent = prefs.getInt("reports_sent", 0)
        // Prefernces helper for limit report count
        val limit = PrefsManager.getReportLimit(requireContext())
        //val limit = prefs.getInt("reports_limit", 1)

        val text = view?.findViewById<TextView>(R.id.text_report_limit)

        text?.text = if (sent >= limit) {
            "Hai raggiunto il limite ($sent/$limit).\nAttendi lo sblocco o nuove disponibilità."
        } else {
            "Segnalazioni disponibili: ${limit - sent}"
        }
    }
}