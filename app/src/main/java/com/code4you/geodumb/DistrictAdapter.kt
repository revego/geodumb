package com.code4you.geodumb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Oggetto per rappresentare un quartiere + numero di segnalazioni
data class DistrictItem(
    val name: String,
    val count: Int
)

class DistrictAdapter(
    private val districts: List<DistrictItem>
) : RecyclerView.Adapter<DistrictAdapter.DistrictViewHolder>() {

    class DistrictViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val districtTextView: TextView = view.findViewById(R.id.district_name)
        val countTextView: TextView = view.findViewById(R.id.district_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistrictViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_district, parent, false)
        return DistrictViewHolder(view)
    }

    override fun onBindViewHolder(holder: DistrictViewHolder, position: Int) {
        val district = districts[position]
        holder.districtTextView.text = district.name
        holder.countTextView.text = "${district.count} segnalazione${if(district.count > 1) "i" else ""}"
    }

    override fun getItemCount() = districts.size
}