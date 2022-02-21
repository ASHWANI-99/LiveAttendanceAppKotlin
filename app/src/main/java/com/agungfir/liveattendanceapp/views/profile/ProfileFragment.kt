package com.agungfir.liveattendanceapp.views.profile

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCALE_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.agungfir.liveattendanceapp.BuildConfig
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.FragmentProfileBinding
import com.agungfir.liveattendanceapp.dialog.MyDialog
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.model.LogoutResponse
import com.agungfir.liveattendanceapp.networking.ApiService
import com.agungfir.liveattendanceapp.views.changepass.ChangePasswordActivity
import com.agungfir.liveattendanceapp.views.login.LoginActivity
import com.agungfir.liveattendanceapp.views.main.MainActivity
import com.bumptech.glide.Glide
import org.jetbrains.anko.startActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        updateView()
    }

    private fun updateView() {
        val user = HawkStorage.instance(requireContext()).getUser()
        val imageUrl = BuildConfig.BASE_IMAGE_URL + user.photo

        Glide.with(requireContext())
            .load(imageUrl)
            .override(100)
            .placeholder(android.R.color.darker_gray)
            .into(binding?.ivProfile!!)

        binding?.apply {
            tvName.text = user.name
            tvEmailProfile.text = user.email
        }
    }

    private fun onClick() {
        binding?.btnChangePassword?.setOnClickListener {
            context?.startActivity<ChangePasswordActivity>()
        }

        binding?.btnChangeLanguage?.setOnClickListener {
            startActivity(Intent(ACTION_LOCALE_SETTINGS))
        }

        binding?.btnLogout?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.log_out))
                .setMessage(getString(R.string.are_you_sure_to_log_out))
                .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                    logoutRequest(dialog)
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun logoutRequest(dialog: DialogInterface) {
        val token = HawkStorage.instance(requireContext()).getToken()

        ApiService.getLiveAttendanceServices()
            .logoutRequest("Bearer $token")
            .enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(
                    call: Call<LogoutResponse>,
                    response: Response<LogoutResponse>
                ) {
                    dialog.dismiss()
                    if (response.isSuccessful) {
                        HawkStorage.instance(requireContext()).deleteAll()
                        (activity as MainActivity).finishAffinity()
                        context?.startActivity<LoginActivity>()
                    } else {
                        MyDialog.dynamicDialog(
                            requireContext(),
                            getString(R.string.alert),
                            getString(R.string.alert)
                        )
                    }
                }

                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    dialog.dismiss()
                    MyDialog.dynamicDialog(
                        requireContext(),
                        getString(R.string.alert),
                        getString(R.string.alert)
                    )
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}