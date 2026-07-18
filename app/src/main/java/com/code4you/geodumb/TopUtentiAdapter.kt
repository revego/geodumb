package com.code4you.geodumb

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.UserContribution


class TopUtentiAdapter : RecyclerView.Adapter<TopUtentiAdapter.ViewHolder>() {

    private var items: List<UserContribution> = emptyList()
    //private var items: List<Pair<Int?, Int>> = emptyList() // (userId, count)

    fun submitList(list: List<UserContribution>) {
        items = list
        Log.d("ADAPTER", "Ricevuti ${items.size} contributor")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_utente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        //val (userId, count) = items[position]
        holder.tvUserName.text = item.userName
        //holder.tvUserName.text = "Utente ${userId ?: "anonimo"}"
        holder.tvUserCount.text = "${item.count} segnalazioni"
        //holder.tvUserCount.text = "$count segnalazioni"
        // Se hai un campo userName, usalo al posto di "Utente $userId"
        // holder.ivAvatar.load(userAvatarUrl) con Glide/Coil
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        val tvUserName: TextView = view.findViewById(R.id.tv_user_name)
        val tvUserCount: TextView = view.findViewById(R.id.tv_user_count)
    }
}