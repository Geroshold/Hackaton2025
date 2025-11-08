package com.example.go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.go.data.JsonPlaceManager
import com.example.go.data.Place
import kotlinx.coroutines.launch

class CatalogFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlacesAdapter
    private val placesList = mutableListOf<Place>()
    private lateinit var jsonPlaceManager: JsonPlaceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_catalog, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        jsonPlaceManager = JsonPlaceManager(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPlacesFromJson()
    }

    private fun setupRecyclerView() {
        adapter = PlacesAdapter(placesList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadPlacesFromJson() {
        lifecycleScope.launch {
            try {
                // Используем JsonPlaceManager для загрузки данных
                val places = jsonPlaceManager.getAllPlaces()

                placesList.clear()
                placesList.addAll(places)
                adapter.notifyDataSetChanged()

                Log.d("CatalogFragment", "Успешно загружено ${placesList.size} мест")

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CatalogFragment", "Ошибка загрузки мест: ${e.message}")
                loadPlacesFallback()
            }
        }
    }

    private fun loadPlacesFallback() {
        try {
            // Старый метод как резервный вариант
            val inputStream = requireContext().assets.open("places.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)

            placesList.clear()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val place = Place(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    description = jsonObject.getString("description"),
                    location = jsonObject.getString("location"),
                    visitors = jsonObject.getInt("visitors"),
                    imageUrl = jsonObject.getString("imageUrl"),
                    points = if (jsonObject.has("points")) jsonObject.getInt("points") else 10 // Значение по умолчанию
                )
                placesList.add(place)
            }

            adapter.notifyDataSetChanged()
            Log.d("CatalogFragment", "Fallback: загружено ${placesList.size} мест")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CatalogFragment", "Ошибка в fallback методе: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на фрагмент
        loadPlacesFromJson()
    }
}