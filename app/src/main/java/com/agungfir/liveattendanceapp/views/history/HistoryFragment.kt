package com.agungfir.liveattendanceapp.views.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.agungfir.liveattendanceapp.R
import com.agungfir.liveattendanceapp.databinding.FragmentHistoryBinding
import com.agungfir.liveattendanceapp.date.MyDate.fromTimeStampToDate
import com.agungfir.liveattendanceapp.date.MyDate.toCalendar
import com.agungfir.liveattendanceapp.date.MyDate.toDate
import com.agungfir.liveattendanceapp.date.MyDate.toDay
import com.agungfir.liveattendanceapp.date.MyDate.toMonth
import com.agungfir.liveattendanceapp.date.MyDate.toTime
import com.agungfir.liveattendanceapp.dialog.MyDialog
import com.agungfir.liveattendanceapp.hawkstorage.HawkStorage
import com.agungfir.liveattendanceapp.model.History
import com.agungfir.liveattendanceapp.model.HistoryResponse
import com.agungfir.liveattendanceapp.networking.ApiService
import com.applandeo.materialcalendarview.EventDay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class HistoryFragment : Fragment() {

    private var binding: FragmentHistoryBinding? = null

    companion object {
        private val TAG: String = HistoryFragment::class.java.simpleName
    }

    private val events = mutableListOf<EventDay>()
    private var dataHistories: List<History>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        // Request Data History
        requestDataHistory()

        // Setup Calendar Swipe
        setupCalendar()

        // OnClick
        oncClick()
    }

    private fun oncClick() {
        binding?.calendarViewHistory?.setOnDayClickListener {
            val clickedDayCalendar = it.calendar
            binding?.layoutTodayCheckInOut?.tvCurrentDate?.text =
                clickedDayCalendar.toDate().toDay()
            binding?.layoutTodayCheckInOut?.tvCurrentMonth?.text =
                clickedDayCalendar.toDate().toMonth()

            if (dataHistories != null) {
                for (dataHistory in dataHistories!!) {
                    val checkInTime: String
                    val checkOutTime: String
                    val updateDate = dataHistory?.updatedAt
                    val calendarUpdated = updateDate?.fromTimeStampToDate()?.toCalendar()


                    if (clickedDayCalendar.get(Calendar.DAY_OF_MONTH) == calendarUpdated?.get(
                            Calendar.DAY_OF_MONTH
                        )
                    ) {
                        if (dataHistory.status == 1) {
                            checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                            checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()

                            binding?.layoutTodayCheckInOut?.tvTimeCheckIn?.text =
                                checkInTime.fromTimeStampToDate()?.toTime()
                            binding?.layoutTodayCheckInOut?.tvTimeCheckOut?.text =
                                checkOutTime.fromTimeStampToDate()?.toTime()
                            break
                        } else {
                            checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                            binding?.layoutTodayCheckInOut?.tvTimeCheckIn?.text =
                                checkInTime.fromTimeStampToDate()?.toTime()
                            break
                        }
                    } else {
                        binding?.layoutTodayCheckInOut?.tvTimeCheckIn?.text =
                            getString(R.string.default_text)
                        binding?.layoutTodayCheckInOut?.tvTimeCheckOut?.text =
                            getString(R.string.default_text)
                    }
                }
            }
        }
    }

    private fun setupCalendar() {
        binding?.calendarViewHistory?.setOnPreviousPageChangeListener {
            requestDataHistory()
        }

        binding?.calendarViewHistory?.setOnForwardPageChangeListener {
            requestDataHistory()
        }
    }

    private fun requestDataHistory() {
        val calendar = binding?.calendarViewHistory?.currentPageDate
        val lastDay = calendar?.getActualMaximum(Calendar.DAY_OF_MONTH)
        val month = calendar?.get(Calendar.MONTH)?.plus(1)
        val year = calendar?.get(Calendar.YEAR)

        val fromDate = "$year-$month-01"
        val toDate = "$year-$month-$lastDay"
        getDataHistory(fromDate, toDate)
    }

    private fun getDataHistory(fromDate: String, toDate: String) {
        val token = HawkStorage.instance(requireContext()).getToken()
        binding?.pbHistory?.visibility = View.VISIBLE
        ApiService.getLiveAttendanceServices()
            .getHistoryAttendance("Bearer $token", fromDate, toDate)
            .enqueue(object : Callback<HistoryResponse> {
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    binding?.pbHistory?.visibility = View.GONE
                    if (response.isSuccessful) {
                        dataHistories = response.body()?.histories as List<History>
                        if (dataHistories != null) {
                            for (dataHistory in dataHistories!!) {
                                val status = dataHistory.status
                                val checkInTime: String
                                val checkOutTime: String
                                val calendarHistoryCheckIn: Calendar?
                                val calendarHistoryCheckOut: Calendar?
                                val currentDate = Calendar.getInstance()

                                if (status == 1) {
                                    checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                    checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()

                                    calendarHistoryCheckOut =
                                        checkOutTime.fromTimeStampToDate()?.toCalendar()

                                    if (calendarHistoryCheckOut != null) {
                                        events.add(
                                            EventDay(
                                                calendarHistoryCheckOut,
                                                R.drawable.ic_check_primary
                                            )
                                        )
                                    }
                                    if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckOut?.get(
                                            Calendar.DAY_OF_MONTH
                                        )
                                    ) {
                                        binding?.apply {
                                            layoutTodayCheckInOut.apply {
                                                tvCurrentDate.text =
                                                    checkInTime.fromTimeStampToDate()?.toDay()
                                                tvCurrentMonth.text =
                                                    checkInTime.fromTimeStampToDate()?.toMonth()
                                                tvTimeCheckIn.text =
                                                    checkInTime.fromTimeStampToDate()?.toTime()
                                                tvTimeCheckOut.text =
                                                    checkOutTime.fromTimeStampToDate()?.toTime()
                                            }
                                        }
                                    }
                                } else {
                                    checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                    calendarHistoryCheckIn =
                                        checkInTime.fromTimeStampToDate()?.toCalendar()

                                    if (calendarHistoryCheckIn != null) {
                                        events.add(
                                            EventDay(
                                                calendarHistoryCheckIn,
                                                R.drawable.ic_check_yellow
                                            )
                                        )
                                    }

                                    if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckIn?.get(
                                            Calendar.DAY_OF_MONTH
                                        )
                                    ) {
                                        binding?.layoutTodayCheckInOut?.tvCurrentDate?.text =
                                            checkInTime.fromTimeStampToDate()?.toDay()
                                        binding?.layoutTodayCheckInOut?.tvCurrentMonth?.text =
                                            checkInTime.fromTimeStampToDate()?.toMonth()
                                        binding?.layoutTodayCheckInOut?.tvTimeCheckIn?.text =
                                            checkInTime.fromTimeStampToDate()?.toTime()
                                    }
                                }
                            }
                        }
                        binding?.calendarViewHistory?.setEvents(events)
                    } else {
                        MyDialog.dynamicDialog(
                            requireContext(),
                            getString(R.string.alert),
                            getString(R.string.something_went_wrong)
                        )
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    binding?.pbHistory?.visibility = View.GONE
                    MyDialog.dynamicDialog(
                        requireContext(),
                        getString(R.string.alert),
                        "${t.message}"
                    )

                    Log.e(TAG, "Error: ${t.message}")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}