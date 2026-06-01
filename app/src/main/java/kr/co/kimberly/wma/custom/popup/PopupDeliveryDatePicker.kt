package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.PopupDatePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class PopupDeliveryDatePicker(val mContext: Context): Dialog(mContext) {
        private lateinit var mBinding: PopupDatePickerBinding
        private val today: Calendar = Calendar.getInstance()
        private var year: Int = today.get(Calendar.YEAR)
        private var month: Int = today.get(Calendar.MONTH) + 1
        private var date: Int = today.get(Calendar.DATE)

        var onSelectedDate: ((String) -> Unit)? = null

        @SuppressLint("SetTextI18n")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mBinding = PopupDatePickerBinding.inflate(layoutInflater)
            setContentView(mBinding.root)

            initView()
        }

        private fun doDayOfWeek(): String {
            return when (today.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "일"
                Calendar.MONDAY -> "월"
                Calendar.TUESDAY -> "화"
                Calendar.WEDNESDAY -> "수"
                Calendar.THURSDAY -> "목"
                Calendar.FRIDAY -> "금"
                Calendar.SATURDAY -> "토"
                else -> ""
            }
        }

        private fun onEditTextChange() {
            mBinding.year.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val yearStr = mBinding.year.text.toString()
                    if (yearStr.isNotEmpty()) {
                        year = Integer.parseInt(mBinding.year.text.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            mBinding.month.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val monthStr = mBinding.month.text.toString()
                    if (monthStr.isNotEmpty()) {
                        val currentInputMonth = monthStr.toIntOrNull()
                        if (currentInputMonth != null && currentInputMonth > 12) {
                            mBinding.month.setText("12")
                            mBinding.month.setSelection(mBinding.month.text.length)
                        }
                        month = Integer.parseInt(mBinding.month.text.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            mBinding.date.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val dateStr = mBinding.date.text.toString()
                    if (dateStr.isNotEmpty()) {
                        val currentInputDate = dateStr.toIntOrNull()
                        if (currentInputDate != null) {
                            val getLastDate = Calendar.getInstance()
                            getLastDate.set(year, month - 1, 1)
                            val currentMaxDate = getLastDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                            if (currentInputDate > currentMaxDate) {
                                mBinding.date.setText(currentMaxDate.toString())
                                mBinding.date.setSelection(mBinding.date.text.length)
                            }
                        }
                        date = Integer.parseInt(mBinding.date.text.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

    private fun calcDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.time

        // 선택된 날짜
        val selectedDate = today.time

        // 연, 월, 일 단위로만 비교
        val tomorrowCalendar = Calendar.getInstance()
        tomorrowCalendar.time = tomorrow

        return if (selectedDate.before(tomorrow)) {
            tomorrowCalendar.time
        } else {
            selectedDate
        }
    }


    private fun onButtonClick() {
        // 년도 조절 버튼
        mBinding.btnAddYear.setOnClickListener {
            year += 1
            updateDate()
        }

        mBinding.btnMinusYear.setOnClickListener {
            year -= 1
            updateDate()
        }

        /*// 월 조절 버튼
        mBinding.btnAddMonth.setOnClickListener {
            month += 1
            updateDate()
        }

        mBinding.btnMinusMonth.setOnClickListener {
            month -= 1
            updateDate()
        }

        // 일 조절 버튼
        mBinding.btnAddDate.setOnClickListener {
            date += 1
            updateDate()
        }

        mBinding.btnMinusDate.setOnClickListener {
            date -= 1
            updateDate()
        }*/

        // 월 조절 버튼
        mBinding.btnAddMonth.setOnClickListener {
            month += 1
            updateDate(true) // 월이 변경되었음을 알림
        }

        mBinding.btnMinusMonth.setOnClickListener {
            month -= 1
            updateDate(true) // 월이 변경되었음을 알림
        }

        // 일 조절 버튼
        mBinding.btnAddDate.setOnClickListener {
            date += 1
            updateDate()
        }

        mBinding.btnMinusDate.setOnClickListener {
            date -= 1
            updateDate()
        }

    }
    private fun updateDate(isMonthChange: Boolean = false) {
        today.set(year, month - 1, date)

        val currentDate = Calendar.getInstance()
        currentDate.add(Calendar.DAY_OF_YEAR, 0)
        if (today.before(currentDate)) {
            today.time = currentDate.time
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH) + 1
            date = today.get(Calendar.DATE)
        }

        // 월을 변경했을 때 `date`가 새로운 월의 최대 날짜를 넘지 않도록 설정
        if (isMonthChange) {
            val maxDateForNextMonth = getEndDate(month)
            if (date > maxDateForNextMonth) {
                date = 1 // 다음 달의 첫날로 설정
            }
        }

        // 날짜가 현재 월의 최대 날짜를 초과할 경우 다음 월로 이동
        val maxDateForCurrentMonth = getEndDate(month)
        if (date > maxDateForCurrentMonth) {
            date = 1
            month += 1
        }

        // 월이 12월을 초과하면 년도를 증가시키고 1월로 설정
        if (month > 12) {
            year += 1
            month = 1
        }

        // 월이 1월보다 작아지면 년도를 감소시키고 12월로 설정
        if (month < 1) {
            year -= 1
            month = 12
        }

        // 날짜가 1일보다 작아지면 이전 달의 마지막 날짜로 설정
        if (date < 1) {
            month -= 1
            if (month < 1) {
                year -= 1
                month = 12
            }
            date = getEndDate(month)
        }

        mBinding.year.setText(year.toString())
        mBinding.month.setText(month.toString().padStart(2, '0'))
        mBinding.date.setText(date.toString().padStart(2, '0'))
    }



    /*private fun updateDate() {
        today.set(year, month - 1, date)

        val currentDate = Calendar.getInstance()
        currentDate.add(Calendar.DAY_OF_YEAR, 1)
        if (today.before(currentDate)) {
            today.time = currentDate.time
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH) + 1
            date = today.get(Calendar.DATE)
        }

        if (date > getEndDate(month)) {
            date = 1
            month += 1
        }

        if (month > 12) {
            year += 1
            month = 1
        }

        if (month < 1) {
            year -= 1
            month = 12
        }

        if (date < 1) {
            month -= 1
            date = getEndDate(month)

        }

        mBinding.year.setText(year.toString())
        mBinding.month.setText(month.toString().padStart(2, '0'))
        mBinding.date.setText(date.toString().padStart(2, '0'))
    }*/

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        private fun initView() {
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            mBinding.title.text = "${year}년 ${month}월 ${date}일 ${doDayOfWeek()}요일"

            updateDate()
            onButtonClick()
            onEditTextChange()

            // 취소 버튼 클릭 시
            mBinding.cancelBtn.setOnClickListener {
                dismiss()
            }

            // 완료 버튼 클릭 시
            mBinding.confirmBtn.setOnClickListener {
                updateDate()
                val dateFormat = SimpleDateFormat("yyyy/MM/dd")
                onSelectedDate?.invoke(dateFormat.format(calcDate()))
                dismiss()
            }
        }

        private fun getEndDate(currentMonth: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, currentMonth - 1)
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
