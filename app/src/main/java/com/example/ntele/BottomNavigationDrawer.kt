package com.example.ntele

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ntele.databinding.ActivityBottomNavigationDrawerBinding
import com.example.ntele.fragments.HomeFragment
import com.example.ntele.fragments.ServiceFragment
import com.example.ntele.fragments.VehicleFragment

class BottomNavigationDrawer : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavigationDrawerBinding
    private var name: String? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavigationDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        name = intent.getStringExtra("name")
        Log.d(" name Navigation","" + name)
        val fragmentToOpen = intent.getStringExtra("openFragment")?: "home"

        when (fragmentToOpen) {
            "vehicle" -> {
                replaceFragment(VehicleFragment())
                binding.customNav.setSelectedItem(1)
            }
            "service" -> {
                replaceFragment(ServiceFragment())
                binding.customNav.setSelectedItem(2)
            }
            else -> {
                replaceFragment(HomeFragment(),name)
                binding.customNav.setSelectedItem(0)
            }
        }

        // Bottom nav bar item selection
        binding.customNav.setOnItemSelectedListener { index ->
            when (index) {
                0 -> replaceFragment(HomeFragment(),name)
                1 -> replaceFragment(VehicleFragment())
                2 -> replaceFragment(ServiceFragment())
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, name: String? = null) {
        if (name != null && fragment is HomeFragment) {
            val bundle = Bundle()
            bundle.putString("name", name)
            fragment.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}
