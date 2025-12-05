package com.code4you.geodumb

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

class FragmentContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        val toolbar = findViewById<Toolbar>(R.id.fragment_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Mostra freccia "Indietro"

        val fragmentName = intent.getStringExtra("FRAGMENT_NAME")
        if (fragmentName != null && savedInstanceState == null) {
            val fragment = createFragmentByName(fragmentName)
            if (fragment != null) {
                supportActionBar?.title = getTitleForFragment(fragmentName)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }

    private fun createFragmentByName(name: String): Fragment? {
        return when (name) {
            // Ora questo funziona perché la classe è stata importata
            "PROFILE" -> ProfileFragment()
            // Aggiungi qui altri casi per futuri fragment
            // "SETTINGS" -> SettingsFragment()
            // "MY_PLACES" -> MyPlacesFragment()
            else -> null
        }
    }

    private fun getTitleForFragment(name: String): String {
        return when (name) {
            "PROFILE" -> "Profilo Utente"
            "SETTINGS" -> "Impostazioni"
            "MY_PLACES" -> "I Miei Luoghi"
            else -> "GeoDumb"
        }
    }

    // Gestisce il click sulla freccia "Indietro" nella toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
