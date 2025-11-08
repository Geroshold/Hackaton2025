package com.example.go

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Устанавливаем слушатель для навигации
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_catalog -> {
                    replaceFragment(CatalogFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                R.id.navigation_rating -> {
                    replaceFragment(RatingFragment())
                    true
                }
                R.id.navigation_scanner -> {
                    replaceFragment(ScannerFragment())
                    true
                }
                else -> false
            }
        }

        // Устанавливаем начальный фрагмент
        if (savedInstanceState == null) {
            replaceFragment(CatalogFragment())
            bottomNavigationView.selectedItemId = R.id.navigation_catalog
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}