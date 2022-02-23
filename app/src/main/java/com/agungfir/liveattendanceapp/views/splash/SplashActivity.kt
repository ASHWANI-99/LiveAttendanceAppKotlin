package com.agungfir.liveattendanceapp.views.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.views.login.LoginActivity
import com.agungfir.liveattendanceapp.views.main.MainActivity
import org.jetbrains.anko.startActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        afterDelayGotoLogin()
    }

    private fun afterDelayGotoLogin() {
        Handler(mainLooper)
            .postDelayed({
                checkIsLogin()
            }, 2000)
    }

    private fun checkIsLogin() {
        val isLogin = HawkStorage.instance(this).isLogin()
        if (isLogin) {
            startActivity<MainActivity>()
            finishAffinity()
        } else {
            startActivity<LoginActivity>()
            finishAffinity()
        }
    }
}