package com.example.go

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.go.data.Place

class PlacesAdapter(
    private val places: List<Place>
) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivPlaceImage)
        val nameTextView: TextView = itemView.findViewById(R.id.tvPlaceName)
        val locationTextView: TextView = itemView.findViewById(R.id.tvLocation)
        val visitorsTextView: TextView = itemView.findViewById(R.id.tvVisitors)
        val pointsTextView: TextView = itemView.findViewById(R.id.tvPoints) // Добавляем TextView для баллов
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_card, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]

        // Загружаем изображение
        Glide.with(holder.itemView.context)
            .load(place.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        // Устанавливаем тексты
        holder.nameTextView.text = place.name
        holder.locationTextView.text = place.location
        holder.visitorsTextView.text = formatVisitors(place.visitors)
        holder.pointsTextView.text = "💰 ${place.points} баллов" // Отображаем баллы
    }

    override fun getItemCount(): Int = places.size

    // Форматирует текст с количеством посетителей
    private fun formatVisitors(count: Int): String {
        return when {
            count == 0 -> "Пока нет посетителей"
            count % 10 == 1 && count % 100 != 11 -> "$count посетитель"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count посетителя"
            else -> "$count посетителей"
        }
    }
}