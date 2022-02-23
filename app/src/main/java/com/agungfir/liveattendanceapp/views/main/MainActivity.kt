package com.agungfir.liveattendanceapp.views.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.ActivityMainBinding
import com.agungfir.liveattendanceapp.views.history.HistoryFragment
import com.agungfir.liveattendanceapp.views.home.HomeFragment
import com.agungfir.liveattendanceapp.views.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fragmentTransaction: FragmentTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        binding.btmNavMain.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_history -> {
                    openFragment(HistoryFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_home -> {
                    openFragment(HomeFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_profile -> {
                    openFragment(ProfileFragment())
                    return@setOnNavigationItemSelectedListener true
                }
            }
            return@setOnNavigationItemSelectedListener false
        }
        openHomeFragment()
    }

    private fun openHomeFragment() {
        binding.btmNavMain.selectedItemId = R.id.action_home
    }

    private fun openFragment(fragment: Fragment) {
        fragmentTransaction = supportFragmentManager.beginTransaction()

        when (supportFragmentManager.findFragmentByTag(fragment.tag)) {
            !is HomeFragment -> {
                fragmentTransaction
                    .replace(R.id.frame_main, fragment, HomeFragment::class.java.simpleName)
                    .commit()
            }
            !is HistoryFragment -> {
                fragmentTransaction
                    .replace(R.id.frame_main, fragment, HistoryFragment::class.java.simpleName)
                    .commit()
            }
            !is ProfileFragment -> {
                fragmentTransaction
                    .replace(R.id.frame_main, fragment, ProfileFragment::class.java.simpleName)
                    .commit()
            }
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}