package com.example.go.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class JsonPlaceManager(private val context: Context) {

    private val gson = Gson()
    private var cachedPlaces: List<Place> = emptyList()

    // Основной метод загрузки всех мест из JSON файла
    suspend fun loadAllPlaces(): List<Place> {
        if (cachedPlaces.isNotEmpty()) {
            return cachedPlaces
        }

        return try {
            val jsonString = loadJsonFromAssets("places.json")
            val listType = object : TypeToken<List<Place>>() {}.type
            val places = gson.fromJson<List<Place>>(jsonString, listType)
            cachedPlaces = places ?: emptyList()
            cachedPlaces
        } catch (e: Exception) {
            android.util.Log.e("JsonPlaceManager", "Ошибка загрузки мест: ${e.message}")
            emptyList()
        }
    }

    // Получение места по ID
    suspend fun getPlaceById(placeId: String): Place? {
        val places = loadAllPlaces()
        return places.find { it.id == placeId }
    }

    // Получение места по имени (для поиска)
    suspend fun getPlaceByName(placeName: String): Place? {
        val places = loadAllPlaces()
        return places.find { it.name == placeName }
    }

    // Получение всех мест
    suspend fun getAllPlaces(): List<Place> {
        return loadAllPlaces()
    }

    // Извлечение ID места из URL
    fun extractPlaceIdFromUrl(url: String): String? {
        return when {
            url.startsWith("yourapp://place/") -> url.removePrefix("yourapp://place/")
            url.startsWith("goapp://place/") -> url.removePrefix("goapp://place/")
            url.startsWith("omsk://place/") -> url.removePrefix("omsk://place/")
            url.contains("#") -> url.substringAfterLast("#")
            url.contains("id=") -> {
                val params = url.substringAfter("?").split("&")
                params.find { it.startsWith("id=") }?.substringAfter("id=")
            }
            else -> {
                // Проверяем, является ли текст прямым ID из нашего списка
                if (isValidPlaceId(url)) url else null
            }
        }
    }

    // Проверка валидности ID места
    private fun isValidPlaceId(placeId: String): Boolean {
        return placeId.isNotEmpty() && !placeId.startsWith("http") && !placeId.contains(" ")
    }

    // Загрузка JSON из папки assets
    private suspend fun loadJsonFromAssets(fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.assets.open(fileName)
                inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                throw Exception("Ошибка загрузки файла $fileName из assets: ${e.message}")
            }
        }
    }

    // Поиск мест по запросу
    suspend fun searchPlaces(query: String): List<Place> {
        val places = loadAllPlaces()
        return places.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)
        }
    }

    // Получение количества мест
    suspend fun getPlacesCount(): Int {
        val places = loadAllPlaces()
        return places.size
    }

    // Получение популярных мест (по количеству посетителей)
    suspend fun getPopularPlaces(limit: Int = 5): List<Place> {
        val places = loadAllPlaces()
        return places.sortedByDescending { it.visitors }.take(limit)
    }

    // Очистка кэша
    fun clearCache() {
        cachedPlaces = emptyList()
    }

    // Проверка существования места
    suspend fun placeExists(placeId: String): Boolean {
        val places = loadAllPlaces()
        return places.any { it.id == placeId }
    }

    // Обновление счетчика посетителей
    suspend fun incrementVisitorCount(placeId: String) {
        val places = loadAllPlaces()
        val updatedPlaces = places.map { place ->
            if (place.id == placeId) {
                place.copy(visitors = place.visitors + 1)
            } else {
                place
            }
        }
        cachedPlaces = updatedPlaces
    }
}