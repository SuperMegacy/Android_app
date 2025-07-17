package com.example.studentapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.studentapp.data.ui.viewmodels.NoteListViewModel
import com.example.studentapp.databinding.ActivityMainBinding



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    fun getNoteListViewModel(): NoteListViewModel {
        return ViewModelProvider(this)[NoteListViewModel::class.java]
    }

    private fun setupNavigation() {
        // Get NavHostFragment and its NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Optional: If you add a BottomNavigationView later
        // binding.bottomNav.setupWithNavController(navController)
    }

    // Optional: Handle back button press
    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment_container).navigateUp()
                || super.onSupportNavigateUp()
    }
}