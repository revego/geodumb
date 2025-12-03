package com.code4you.geodumb

import com.code4you.geodumb.R
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class DescriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)


        // Impostiamo il testo della descrizione
        //val descriptionTextView = findViewById<TextView>(R.id.description_text_view)
        //val description =
            "Citylog è un'app progettata per visualizzare e gestire facilmente segnalazioni geografiche tramite immagini e informazioni di posizione. Organizza e accedi alle tue foto archiviate con semplicità"
        //descriptionTextView.text = description
    }
}
