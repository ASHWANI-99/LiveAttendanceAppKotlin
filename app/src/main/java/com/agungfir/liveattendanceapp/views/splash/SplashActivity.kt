package com.agungfir.liveattendanceapp.views.splash

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.views.login.LoginActivity
import org.jetbrains.anko.startActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        afterDelayGotoLogin()
    }

    private fun afterDelayGotoLogin() {
        Handler(mainLooper)
            .postDelayed({
                startActivity<LoginActivity>()
                finishAffinity()
            }, 2000)
    }
}