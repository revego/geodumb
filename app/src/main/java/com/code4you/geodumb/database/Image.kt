package com.code4you.geodumb.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class Image(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // ID univoco
    val imagePath: String, // Percorso dell'immagine
    val description: String?, // Descrizione opzionale
    val date: String?, // Descrizione opzionale
    val address: String?, // Descrizione opzionale
    val path: String?
)
