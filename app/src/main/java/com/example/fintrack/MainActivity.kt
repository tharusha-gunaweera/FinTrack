package com.example.fintrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fintrack.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(DashboardFragment())

        binding.bottomNavigation.setOnItemSelectedListener {

            when(it.itemId){
                R.id.nav_dashboard -> replaceFragment(DashboardFragment())
                R.id.nav_transactions -> replaceFragment(TransactionFragment())
                R.id.nav_reports -> replaceFragment(ReportsFragment())
                R.id.nav_settings -> replaceFragment(Settings())

                else -> {


                }
            }
            true
        }

    }



    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()

    }
}
