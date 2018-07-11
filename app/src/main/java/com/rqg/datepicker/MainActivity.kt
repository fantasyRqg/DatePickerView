package com.rqg.reatil.datepicker

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.rqg.github.datepiker.DatePickCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : Activity(), DatePickCallback {

    val TAG = "MainActivity"

    override fun onDateSelect(year: Int?, month: Int?, dayOfMonth: Int?) {
        Log.d(TAG, "onDateSelect() called with: year = [ ${year} ], month = [ ${month} ], dayOfMonth = [ ${dayOfMonth} ]")
    }

    override fun onDateRangeSelect(startYear: Int?, startMonth: Int?, startDaysOfMonth: Int?, endYear: Int?, endMonth: Int?, endDaysOfMonth: Int?) {
        Log.d(TAG, "onDateRangeSelect() called with: startYear = [ ${startYear} ], startMonth = [ ${startMonth} ], startDaysOfMonth = [ ${startDaysOfMonth} ], endYear = [ ${endYear} ], endMonth = [ ${endMonth} ], endDaysOfMonth = [ ${endDaysOfMonth} ]")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val calendar = Calendar.getInstance(Locale.getDefault())
        date_picker.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))

        date_picker.setDatePickCallback(this)

    }
}
