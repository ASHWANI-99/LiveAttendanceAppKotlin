package com.agungfir.liveattendanceapp.views.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.agungfir.liveattendanceapp.databinding.ActivityLoginBinding
import com.agungfir.liveattendanceapp.views.forgotpass.ForgotPasswordActivity
import com.agungfir.liveattendanceapp.views.main.MainActivity
import org.jetbrains.anko.startActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onClick()
    }

    private fun onClick() {
        binding.btnLogin.setOnClickListener {
            startActivity<MainActivity>()
        }

        binding.btnForgotPassword.setOnClickListener {
            startActivity<ForgotPasswordActivity>()
        }
    }
}