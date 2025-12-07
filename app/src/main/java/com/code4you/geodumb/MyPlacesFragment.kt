package com.code4you.geodumb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class MyPlacesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Collega il layout XML a questa classe
        return inflater.inflate(R.layout.fragment_my_places, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Qui, in futuro, configurerai il RecyclerView per mostrare la lista
        // dei luoghi salvati dall'utente.
        val recyclerView = view.findViewById<RecyclerView>(R.id.my_places_recyclerview)
        // ... logica per popolare la lista ...
    }
}
