package com.code4you.geodumb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.data.MyPlace
import com.squareup.picasso.Picasso

class MyPlacesAdapter(private val places: List<MyPlace>) : RecyclerView.Adapter<MyPlacesAdapter.PlaceViewHolder>() {

    // Crea il ViewHolder che contiene i riferimenti alle View del singolo item
    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cityTextView: TextView = view.findViewById(R.id.place_city)
        val addressTextView: TextView = view.findViewById(R.id.place_address)
        val dateTextView: TextView = view.findViewById(R.id.place_date)
        val imageView: ImageView = view.findViewById(R.id.place_image)
    }

    // "Gonfia" il layout del singolo item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_place, parent, false)
        return PlaceViewHolder(view)
    }

    // "Collega" i dati di un singolo 'MyPlace' alle View del ViewHolder
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.cityTextView.text = place.cityName
        holder.addressTextView.text = place.address
        holder.dateTextView.text = place.date

        // Usa Picasso (che hai già) per caricare l'immagine dall'URL
        Picasso.get()
            .load(place.imageUrl)
            .placeholder(R.drawable.ic_launcher_background) // Immagine di default
            .error(R.drawable.ic_launcher_background)       // In caso di errore
            .into(holder.imageView)
    }

    // Restituisce il numero totale di elementi nella lista
    override fun getItemCount() = places.size
}
