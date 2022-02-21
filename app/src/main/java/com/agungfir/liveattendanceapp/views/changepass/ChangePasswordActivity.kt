package com.agungfir.liveattendanceapp.views.changepass

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.ActivityChangePasswordBinding
import com.agungfir.liveattendanceapp.dialog.MyDialog
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.model.ChangePasswordResponse
import com.agungfir.liveattendanceapp.networking.ApiService
import com.agungfir.liveattendanceapp.networking.RetrofitClient
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    companion object {
        private val TAG = ChangePasswordActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        onClick()
    }

    private fun onClick() {
        binding.tbChangePassword.setOnClickListener {
            finish()
        }

        binding.btnChangePassword.setOnClickListener {
            val oldPass = binding.etOldPassword.text.toString()
            val newPass = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (checkValidation(oldPass, newPass, confirmPassword)) {
                changePasswordPassToServer(oldPass, newPass, confirmPassword)
            }
        }
    }

    private fun changePasswordPassToServer(
        oldPass: String,
        newPass: String,
        confirmPassword: String
    ) {
        val token = HawkStorage.instance(this@ChangePasswordActivity).getToken()
        val changePassRequest = ChangePasswordRequest(oldPass, newPass, confirmPassword)
        val changePassRequestString = Gson().toJson(changePassRequest)

        MyDialog.showProgressDialog(this@ChangePasswordActivity)
        ApiService.getLiveAttendanceServices()
            .changePassRequest("Bearer $token", changePassRequestString)
            .enqueue(object : Callback<ChangePasswordResponse> {
                override fun onResponse(
                    call: Call<ChangePasswordResponse>,
                    response: Response<ChangePasswordResponse>
                ) {
                    MyDialog.hideDialog()
                    if (response.isSuccessful) {
                        MyDialog.dynamicDialog(
                            this@ChangePasswordActivity, getString(R.string.success), getString(
                                R.string.your_password_has_been_update
                            )
                        )
                        Handler(mainLooper).postDelayed(
                            {
                                MyDialog.hideDialog()
                                finish()
                            }, 2000
                        )
                    } else {
                        val errorConverter: Converter<ResponseBody, ChangePasswordResponse> =
                            RetrofitClient
                                .getClient()
                                .responseBodyConverter(
                                    ChangePasswordResponse::class.java,
                                    arrayOfNulls<Annotation>(0)
                                )

                        val errorResponse: ChangePasswordResponse?
                        try {
                            response.errorBody()?.let {
                                errorResponse = errorConverter.convert(it)
                                MyDialog.dynamicDialog(
                                    this@ChangePasswordActivity,
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

                override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                    MyDialog.hideDialog()
                    MyDialog.dynamicDialog(
                        this@ChangePasswordActivity,
                        getString(R.string.alert),
                        "Error: ${t.message}"
                    )
                    Log.e(TAG, "Error: ${t.message}")
                }
            })
    }


    private fun checkValidation(
        oldPass: String,
        newPass: String,
        confirmPassword: String
    ): Boolean {
        if (oldPass.isEmpty()) {
            binding.apply {
                etOldPassword.error = getString(R.string.please_field_your_password)
                etOldPassword.requestFocus()
            }
        } else if (newPass.isEmpty()) {
            binding.apply {
                etNewPassword.error = getString(R.string.please_field_your_password)
                etNewPassword.requestFocus()
            }
        } else if (confirmPassword.isEmpty()) {
            binding.apply {
                etConfirmPassword.error = getString(R.string.please_field_your_password)
                etConfirmPassword.requestFocus()
            }
        } else if (newPass != confirmPassword) {
            binding.apply {
                etNewPassword.error = getString(R.string.your_password_did_not_match)
                etNewPassword.requestFocus()
                etConfirmPassword.error = getString(R.string.your_password_did_not_match)
                etConfirmPassword.requestFocus()
            }
        } else {
            binding.apply {
                etOldPassword.error = null
                etNewPassword.error = null
                etConfirmPassword.error = null
            }
            return true
        }
        return false
    }

    private fun init() {
        setSupportActionBar(binding.tbChangePassword)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}