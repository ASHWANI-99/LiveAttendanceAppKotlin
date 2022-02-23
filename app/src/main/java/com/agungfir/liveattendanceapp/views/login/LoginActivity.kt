package com.agungfir.liveattendanceapp.views.login

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.ActivityLoginBinding
import com.agungfir.liveattendanceapp.dialog.MyDialog
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.model.LoginResponse
import com.agungfir.liveattendanceapp.networking.ApiService
import com.agungfir.liveattendanceapp.networking.RetrofitClient
import com.agungfir.liveattendanceapp.views.forgotpass.ForgotPasswordActivity
import com.agungfir.liveattendanceapp.views.main.MainActivity
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.jetbrains.anko.startActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        private val TAG = LoginActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onClick()
    }

    private fun onClick() {
        binding.apply {
            btnLogin.setOnClickListener {
                val email = binding.etEmailLogin.text.toString()
                val password = binding.etPasswordLogin.text.toString()

                if (isFormValid(email, password)) {
                    loginToServer(email, password)
                }

            }

            btnForgotPassword.setOnClickListener {
                startActivity<ForgotPasswordActivity>()
            }
        }

    }

    private fun isFormValid(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmailLogin.apply {
                error = getString(R.string.please_field_your_email)
                requestFocus()
            }
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailLogin.apply {
                error = context.getString(R.string.please_use_valid_email)
                requestFocus()
            }
        } else if (password.isEmpty()) {
            binding.etEmailLogin.error = null
            binding.etPasswordLogin.apply {
                error = getString(R.string.please_field_your_email)
                requestFocus()
            }
        } else {
            binding.apply {
                etEmailLogin.error = null
                etPasswordLogin.error = null
            }
            return true
        }
        return false
    }

    private fun loginToServer(email: String, password: String) {
        val loginRequest = LoginRequest(email, password, "mobile")
        val loginRequestString = Gson().toJson(loginRequest)

        MyDialog.showProgressDialog(this)

        ApiService.getLiveAttendanceServices()
            .loginRequest(loginRequestString)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    MyDialog.hideDialog()
                    if (response.isSuccessful) {
                        val user = response.body()?.user
                        val token = response.body()?.meta?.token

                        if (user != null && token != null) {
                            HawkStorage.instance(this@LoginActivity).setUser(user)
                            HawkStorage.instance(this@LoginActivity).setToken(token)
                            gotoMain()
                        }
                    } else {
                        val errorConverter: Converter<ResponseBody, LoginResponse> = RetrofitClient
                            .getClient()
                            .responseBodyConverter(
                                LoginResponse::class.java,
                                arrayOfNulls<Annotation>(0)
                            )

                        val errorResponse: LoginResponse?
                        try {
                            response.errorBody()?.let {
                                errorResponse = errorConverter.convert(it)
                                MyDialog.dynamicDialog(
                                    this@LoginActivity,
                                    getString(R.string.failed),
                                    errorResponse?.message.toString()
                                )
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e(TAG, "Error : ${e.message.toString()}")
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    MyDialog.hideDialog()
                    Log.e(TAG, "Error: ${t.message}")
                }
            })

    }

    private fun gotoMain() {
        startActivity<MainActivity>()
        finishAffinity()
    }

}