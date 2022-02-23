package com.agungfir.liveattendanceapp.views.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.agungfir.liveattendanceapp.BuildConfig
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.BottomSheetHomeBinding
import com.agungfir.liveattendanceapp.databinding.FragmentHomeBinding
import com.agungfir.liveattendanceapp.date.MyDate
import com.agungfir.liveattendanceapp.dialog.MyDialog
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.model.AttendanceResponse
import com.agungfir.liveattendanceapp.model.HistoryResponse
import com.agungfir.liveattendanceapp.networking.ApiService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {


    companion object {
        private const val REQUEST_CODE_MAP_PERMISSION = 1000
        private const val REQUEST_CODE_LOCATION = 1001
        private const val REQUEST_CODE_CAMERA_PERMISSION = 2000
        private const val REQUEST_CODE_IMAGE_CAPTURE = 2001
        private val TAG = HomeFragment::class.java.simpleName
    }

    private var currentPhotoPath = ""
    private var isCheckIn = false
    private val mapPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    // Config Maps
    private var mapAttendance: SupportMapFragment? = null
    private var map: GoogleMap? = null
    private var locationManager: LocationManager? = null
    private var settingsClient: SettingsClient? = null
    private var locationSettingRequest: LocationSettingsRequest? = null
    private var locationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var task: Task<LocationSettingsResponse>? = null

    // UI
    private lateinit var binding: FragmentHomeBinding
    private var bindingBottomSheet: BottomSheetHomeBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        bindingBottomSheet = binding.layoutBottomSheet
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        onClick()
    }

    override fun onResume() {
        super.onResume()
        setupMaps()
        checkIfAlreadyPresent()
    }

    private fun onClick() {

        binding.fabCurrentLocation?.setOnClickListener {
            goToCurrentLocation()
        }

        bindingBottomSheet?.apply {
            ivCapturePhoto.setOnClickListener {
                if (checkPermissionCamera()) {
                    openCamera()
                } else {
                    setRequestCamera()
                }
            }

            btnCheckIn.setOnClickListener {
                val token = HawkStorage.instance(requireContext()).getToken()
                if (checkValidation()) {
                    if (isCheckIn()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.are_you_sure))
                            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                sendDataAttendance(token, "out")
                            }
                            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.are_you_sure))
                            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                sendDataAttendance(token, "in")
                            }
                            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun sendDataAttendance(token: String, type: String) {
        val params = HashMap<String, RequestBody>()
        MyDialog.showProgressDialog(requireContext())
        if (currentLocation != null && currentPhotoPath.isNotEmpty()) {
            val latitude = currentLocation?.latitude.toString()
            val longitude = currentLocation?.longitude.toString()
            val address = bindingBottomSheet?.tvCurrentLcoation?.text.toString()

            val file = File(currentPhotoPath)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                BuildConfig.APPLICATION_ID + ".fileprovider",
                file
            )
            val typeFile = context?.contentResolver?.getType(uri)

            val mediaTypeText = MultipartBody.FORM
            val mediaTypeFile = typeFile?.toMediaType()

            val requestLatitude = latitude.toRequestBody(mediaTypeText)
            val requestLongitude = longitude.toRequestBody(mediaTypeText)
            val requestAddress = address.toRequestBody(mediaTypeText)
            val requestType = type.toRequestBody(mediaTypeText)

            params["lat"] = requestLatitude
            params["long"] = requestLongitude
            params["address"] = requestAddress
            params["type"] = requestType

            val requestPhotoFile = file.asRequestBody(mediaTypeFile)
            val multipartBody =
                MultipartBody.Part.createFormData("photo", file.name, requestPhotoFile)
            ApiService.getLiveAttendanceServices()
                .attend("Bearer ${token}", params, multipartBody)
                .enqueue(object : Callback<AttendanceResponse> {
                    override fun onResponse(
                        call: Call<AttendanceResponse>,
                        response: Response<AttendanceResponse>
                    ) {
                        MyDialog.hideDialog()
                        if (response.isSuccessful) {
                            val attendanceResponse = response.body()
                            bindingBottomSheet?.ivCapturePhoto?.setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_photo_camera
                                )
                            )
                            bindingBottomSheet?.ivCapturePhoto?.adjustViewBounds = false

                            if (type == "in") {
                                MyDialog.dynamicDialog(
                                    requireContext(),
                                    getString(R.string.success_check_in),
                                    attendanceResponse?.message.toString()
                                )
                            } else {
                                MyDialog.dynamicDialog(
                                    requireContext(),
                                    getString(R.string.success_check_out),
                                    attendanceResponse?.message.toString()
                                )
                            }
                            checkIfAlreadyPresent()
                        } else {
                            MyDialog.dynamicDialog(
                                requireContext(),
                                getString(R.string.alert),
                                getString(R.string.something_went_wrong)
                            )
                        }
                    }

                    override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                        MyDialog.hideDialog()
                        Log.e(TAG, "Error : ${t.message}")
                    }
                })
        }

    }

    private fun checkIfAlreadyPresent() {
        val token = HawkStorage.instance(requireContext()).getToken()
        val currentDate = MyDate.getCurrentDateForServer()

        ApiService.getLiveAttendanceServices()
            .getHistoryAttendance("Bearer $token", currentDate, currentDate)
            .enqueue(object : Callback<HistoryResponse> {
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        val histories = response.body()?.histories
                        if (histories != null && histories.isNotEmpty()) {
                            if (histories[0]?.status == 1) {

                                isCheckIn = false
                                checkIsCheckIn()
                                bindingBottomSheet?.btnCheckIn?.isEnabled = false
                                bindingBottomSheet?.btnCheckIn?.backgroundTintList =
                                    ContextCompat.getColorStateList(
                                        requireContext(),
                                        android.R.color.darker_gray
                                    )
                                bindingBottomSheet?.btnCheckIn?.text =
                                    getString(R.string.you_already_present)
                            } else {
                                isCheckIn = true
                                checkIsCheckIn()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message}")
                }
            })
    }

    private fun checkIsCheckIn() {
        if (isCheckIn) {
            bindingBottomSheet?.apply {
                btnCheckIn.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.red)
                btnCheckIn.text = getString(R.string.check_out)
            }
        } else {
            bindingBottomSheet?.apply {
                btnCheckIn.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.primary_color)
                btnCheckIn.text = getString(R.string.check_in)
            }
        }
    }

    private fun isCheckIn(): Boolean {
        return isCheckIn
    }

    private fun checkValidation(): Boolean {
        if (currentPhotoPath.isEmpty()) {
            MyDialog.dynamicDialog(
                requireContext(),
                getString(R.string.alert),
                getString(R.string.please_take_your_photo)
            )
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingBottomSheet = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentLocation != null && locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                val uri = Uri.parse(currentPhotoPath)
                bindingBottomSheet?.ivCapturePhoto?.apply {
                    setImageURI(uri)
                    adjustViewBounds = true
                }
            } else {
                if (currentPhotoPath.isNotEmpty()) {
                    val file = File(currentPhotoPath)
                    file.delete()
                    currentPhotoPath = ""
                    context?.toast(getString(R.string.failed_to_capture_image))
                }
            }
        }
    }

    private fun init() {
        // setup Location
        locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
        settingsClient = LocationServices.getSettingsClient(requireContext())
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
        locationSettingRequest = builder.build()

        // setup BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bindingBottomSheet!!.bottomSheetHome)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupMaps() {
        mapAttendance =
            childFragmentManager.findFragmentById(R.id.map_attendance) as SupportMapFragment
        mapAttendance?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (checkPersmission()) {
            // Coordinate My Office is My Home
            val myHome = LatLng(-6.905167, 109.208827)
            map?.addMarker(
                MarkerOptions()
                    .position(myHome)
                    .title("Ketileng, Tegal - Indonesia")
            )
            map?.moveCamera(CameraUpdateFactory.newLatLng(myHome))
            map?.animateCamera(CameraUpdateFactory.zoomTo(20F))

            goToCurrentLocation()
        } else {
            setRequestPermission()
        }
    }

    private fun setRequestPermission() {
        requestPermissions(mapPermissions, REQUEST_CODE_MAP_PERMISSION)
    }

    private fun setRequestCamera() {
        requestPermissions(cameraPermissions, REQUEST_CODE_CAMERA_PERMISSION)
    }

    private fun checkPermissionCamera(): Boolean {
        var isHasPermission = false
        context?.let {
            for (permission in cameraPermissions) {
                isHasPermission = ActivityCompat.checkSelfPermission(
                    it,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isHasPermission
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_MAP_PERMISSION -> {
                var isHasPermission = false
                val permissionNotGranted = StringBuilder()

                for (i in permissions.indices) {
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission) {
                        permissionNotGranted.append("${permissions[i]}\n")
                    }
                }

                if (isHasPermission) {
                    setupMaps()
                } else {
                    val message =
                        "${permissionNotGranted}\n${getString(R.string.not_granted)}"
                    MyDialog.dynamicDialog(
                        requireContext(),
                        getString(R.string.required_permission),
                        message
                    )
                }
            }
            REQUEST_CODE_CAMERA_PERMISSION -> {
                var isHasPermission = false
                val permissionNotGranted = StringBuilder()
                for (i in permissions.indices) {
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission) {
                        permissionNotGranted.append("${permissions[i]}\n")
                    }
                }
                if (isHasPermission) {
                    openCamera()
                } else {
                    val message =
                        "${permissionNotGranted}\n${getString(R.string.not_granted)}"
                    MyDialog.dynamicDialog(
                        requireContext(),
                        getString(R.string.required_permission),
                        message
                    )
                }
            }
        }

    }

    private fun openCamera() {
        context?.let { context ->
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                val photoFile = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        it
                    )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(cameraIntent, REQUEST_CODE_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an Image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("MissingPermission")
    private fun goToCurrentLocation() {
        bindingBottomSheet?.tvCurrentLcoation?.text = getString(R.string.search_your_location)
        if (checkPersmission()) {
            if (isLocationEnabled()) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = false

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        currentLocation = locationResult.lastLocation

                        if (currentLocation != null) {
                            val latitude = currentLocation?.latitude
                            val longitude = currentLocation?.longitude

                            if (latitude != null && longitude != null) {
                                val latlng = LatLng(latitude, longitude)
                                map?.moveCamera(CameraUpdateFactory.newLatLng(latlng))
                                map?.animateCamera(CameraUpdateFactory.zoomTo(20F))

                                val address = getAddress(latitude, longitude)
                                if (address != null && address.isNotEmpty()) {
                                    bindingBottomSheet?.tvCurrentLcoation?.text = address
                                }
                            }
                        }
                    }
                }
                fusedLocationProviderClient?.requestLocationUpdates(
                    locationRequest!!,
                    locationCallback!!,
                    Looper.myLooper()!!
                )
            } else {
                gotoTurnOnGPS()
            }
        } else {
            setRequestPermission()
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String? {
        val result: String
        context?.let {
            val geocode = Geocoder(it, Locale.getDefault())
            val addresses = geocode.getFromLocation(latitude, longitude, 1)

            if (addresses.size > 0) {
                result = addresses[0].getAddressLine(0)
                return result
            }
        }
        return null
    }

    private fun gotoTurnOnGPS() {

        settingsClient?.checkLocationSettings(locationSettingRequest!!)
            ?.addOnSuccessListener {
                goToCurrentLocation()
            }
            ?.addOnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvableException = it as ResolvableApiException
                            resolvableException.startResolutionForResult(
                                requireActivity(),
                                REQUEST_CODE_LOCATION
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                            Log.e(TAG, "Error : ${e.message}")
                        }
                    }
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!! ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)!!
        ) {
            return true
        }
        return false
    }

    private fun checkPersmission(): Boolean {
        var isHasPermission = false
        context?.let {
            for (permission in mapPermissions) {
                isHasPermission = ActivityCompat.checkSelfPermission(
                    it,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isHasPermission
    }

}