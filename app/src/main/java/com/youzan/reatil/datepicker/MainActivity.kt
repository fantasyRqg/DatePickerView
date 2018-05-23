package com.youzan.reatil.datepicker

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val calendar = Calendar.getInstance(Locale.getDefault())
        date_picker.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        date_picker.setDate(2018, 5)
        date_picker.setLineMargin(0)
    }
}
