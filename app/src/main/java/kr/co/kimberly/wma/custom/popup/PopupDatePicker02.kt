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
import kr.co.kimberly.wma.databinding.PopupDatePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class PopupDatePicker02(val mContext: Context, private val isDate: Boolean, private val isStartDate: Boolean): Dialog(mContext) {
    private lateinit var mBinding : PopupDatePickerBinding
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

        initView(isDate)
    }

    private fun doDayOfWeek(): String? {
        var strWeek: String? = null
        when (today.get(Calendar.DAY_OF_WEEK)) {
            1 -> {
                strWeek = "일"
            }
            2 -> {
                strWeek = "월"
            }
            3 -> {
                strWeek = "화"
            }
            4 -> {
                strWeek = "수"
            }
            5 -> {
                strWeek = "목"
            }
            6 -> {
                strWeek = "금"
            }
            7 -> {
                strWeek = "토"
            }
        }
        return strWeek
    }

    private fun onEditTextChange() {
        mBinding.year.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val yearStr = mBinding.year.text.toString()
                if(yearStr.isNotEmpty()) {
                    year = Integer.parseInt(mBinding.year.text.toString())
                }
                year = Integer.parseInt(mBinding.year.text.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        mBinding.month.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val monthStr = mBinding.month.text.toString()
                if(monthStr.isNotEmpty()) {
                    val currentInputMonth = monthStr.toIntOrNull()
                    if (currentInputMonth != null ) {
                        if (currentInputMonth > 12) {
                            mBinding.month.setText(12.toString())
                            mBinding.month.setSelection(mBinding.month.text.length)
                            month = Integer.parseInt(mBinding.month.text.toString())
                        }
                    }
                    month = Integer.parseInt(mBinding.month.text.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        mBinding.date.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val dateStr = mBinding.date.text.toString()
                if(dateStr.isNotEmpty()) {
                    val currentInputDate = dateStr.toIntOrNull()
                    if (currentInputDate != null) {
                        // 해당 월의 마지막 일을 가져와서 그 이상의 날짜를 적으면 해당 월의 마지막 날짜가 입력되도록 변경
                        val getLastDate = Calendar.getInstance()
                        getLastDate.set(year, month - 1, 1)
                        val currentMaxDate = getLastDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (currentInputDate > currentMaxDate) {
                            mBinding.date.setText(currentMaxDate.toString())
                            mBinding.date.setSelection(mBinding.date.text.length)
                            date = Integer.parseInt(mBinding.date.text.toString())
                        }
                    }
                    date = Integer.parseInt(mBinding.date.text.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun calcDate(): Date {
        // 오늘 기준으로 비교를 위해 캘린더를 새로 가져오기
        val calendar = Calendar.getInstance()

        // 오늘 기준 날짜
        val comparedDate = calendar.time

        // 오늘 날짜 기준으로 직전 한달 구하기
        calendar.add(Calendar.MONTH, -1)
        val aMonthBefore = calendar.time

        // 선택된 날짜
        val selectedDate = today.time

        return if (selectedDate < aMonthBefore) {
            showNoticePopup("오늘 기준으로 최대 한달 전의 전표만 조회가 가능합니다.")
            hideDialog()
            calendar.add(Calendar.DAY_OF_YEAR, +1)
            calendar.time
        } else if (selectedDate > comparedDate) {
            showNoticePopup("오늘 기준으로 이전의 날짜의 전표만 조회가 가능합니다.")
            hideDialog()
            comparedDate
        } else {
            hideDialog()
            selectedDate
        }
    }

    private fun onButtonClick(){
        // 년도 조절 버튼
        mBinding.btnAddYear.setOnClickListener {
            year += 1
            updateDate()
        }

        mBinding.btnMinusYear.setOnClickListener {
            year -= 1
            updateDate()
        }

        // 월 조절 버튼
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
        }
    }

    private fun updateDate() {

        today.set(year, month-1, date)

        if (month > 12) {
            year += 1
            month = 1
        }

        if(month < 1) {
            year -= 1
            month = 12
        }

        if (date > getEndDate(month)) {
            month += 1
            date = 1
        }

        if (date < 1) {
            // 월을 빼주기 전에 해당 월의 마지막 날을 date에 할당
            date = getEndDate(month)
            month -= 1
        }

        mBinding.year.setText(year.toString())
        mBinding.month.setText(month.toString().padStart(2, '0'))
        mBinding.date.setText(date.toString().padStart(2, '0'))
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun initView(isDate: Boolean) {
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        mBinding.title.text = "${year}년 ${month}월 ${date}일 ${doDayOfWeek()}요일"

        if (isStartDate) {
            // isStartDate가 true일 때만 date를 오늘 날짜 기준으로 일주일 전으로 설정
            today.add(Calendar.DAY_OF_YEAR, -7)
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH)+1
            date = today.get(Calendar.DATE)
            updateDate()
        } else {
            updateDate()
        }

        onButtonClick()
        onEditTextChange()

        if (isDate) {
            mBinding.layoutDate.visibility = View.GONE
        }

        //  취소 버튼 클릭 시
        mBinding.cancelBtn.setOnClickListener {
            dismiss()
        }

        // 완료 버튼 클릭 시
        mBinding.confirmBtn.setOnClickListener {
            updateDate()
            if (isDate) {
                val dateFormat = SimpleDateFormat("yyyy-MM")
                onSelectedDate?.invoke(dateFormat.format(today.time))
                dismiss()
            } else {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd")
                onSelectedDate?.invoke(dateFormat.format(calcDate()))
            }
        }
    }

    private fun showNoticePopup(msg: String) {
        val popupNotice = PopupNotice(mContext, msg)
        popupNotice.show()
    }

    private fun getEndDate(currentMonth: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, currentMonth - 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun hideDialog() {
        if (isShowing) {
            dismiss()
        }
    }
}