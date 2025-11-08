package com.example.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var totalPointsText: TextView
    private lateinit var totalVisitsText: TextView
    private lateinit var lastVisitText: TextView
    private lateinit var visitedPlacesText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        totalPointsText = view.findViewById(R.id.total_points_text)
        totalVisitsText = view.findViewById(R.id.total_visits_text)
        lastVisitText = view.findViewById(R.id.last_visit_text)
        visitedPlacesText = view.findViewById(R.id.visited_places_text)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateProfileInfo()
    }

    private fun updateProfileInfo() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)

        val totalPoints = sharedPref.getInt("user_points", 0)
        val totalVisits = sharedPref.getInt("total_visits", 0)
        val lastPlace = sharedPref.getString("last_visited_place", "еще нет")
        val lastPoints = sharedPref.getInt("last_visit_points", 0)
        val lastTime = sharedPref.getLong("last_visit_time", 0)

        val history = sharedPref.getStringSet("visit_history", mutableSetOf()) ?: mutableSetOf()
        val visitedCount = history.size

        totalPointsText.text = "💰 Всего баллов: $totalPoints"
        totalVisitsText.text = "📊 Всего посещений: $totalVisits"
        visitedPlacesText.text = "🏛️ Посещено мест: $visitedCount"

        if (lastTime > 0) {
            val date = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastTime))
            lastVisitText.text = "⏰ Последнее посещение: $lastPlace\n+$lastPoints баллов ($date)"
        } else {
            lastVisitText.text = "⏰ Последнее посещение: еще не было"
        }
    }

    override fun onResume() {
        super.onResume()
        updateProfileInfo()
    }
}