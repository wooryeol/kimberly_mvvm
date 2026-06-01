package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.databinding.PopupAccountSearchBinding
import kr.co.kimberly.wma.databinding.PopupDatePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * @param isDate 월을 보여줄 것인지
 * @param currentDate 팝업을 열었을 때 날짜를 선택했는지 확인
 * @param isStartDate 전표조회에서 시작 날짜와 끝 날짜를 구분해주는 값. 사용하지 않을 경우 null
 */
class PopupDatePicker(val mContext: Context, private val isDate: Boolean, private val currentDate: String? = null, private val isStartDate: Boolean? = null): Dialog(mContext) {
    private lateinit var mBinding : PopupDatePickerBinding

    private val today: Calendar = Calendar.getInstance()
    private var year: Int = today.get(Calendar.YEAR)
    private var month: Int = today.get(Calendar.MONTH) + 1
    private var date: Int = today.get(Calendar.DATE)

    var onDateSelect: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupDatePickerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initView(isDate, currentDate)
    }

    // 날짜 갱신 함수 정의
    private fun updateDate() {
        val maxDate = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (date > maxDate) {
            date = maxDate // 선택한 월의 마지막 날짜를 넘어가면 마지막 날짜로 설정
        }
        if(currentDate == null) { // 설정된 값이 없을 경우 오늘 날짜로 설정
            today.set(year, month, date)
        }

        mBinding.year.setText(year.toString())
        mBinding.month.setText(month.toString().padStart(2, '0'))
        mBinding.date.setText(date.toString().padStart(2, '0'))

        mBinding.year.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                
            }

            override fun afterTextChanged(s: Editable?) {
                val yearStr = mBinding.year.text.toString()
                if(yearStr.isNotEmpty()) {
                    year = Integer.parseInt(mBinding.year.text.toString())
                }
            }
        })

        mBinding.month.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                
            }

            override fun afterTextChanged(s: Editable?) {
                val monthStr = mBinding.month.text.toString()
                if(monthStr.isNotEmpty()) {
                    month = Integer.parseInt(mBinding.month.text.toString())
                }
            }
        })

        mBinding.date.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                
            }

            override fun afterTextChanged(s: Editable?) {
                val dateStr = mBinding.date.text.toString()
                if(dateStr.isNotEmpty()) {
                    var currentInputDate = dateStr.toIntOrNull()
                    if (currentInputDate != null) {

                        // 해당 월의 마지막 일을 가져와서 그 이상의 날짜를 적으면 해당 월의 마지막 날짜가 입력되도록 변경
                        val today = Calendar.getInstance()
                        today.set(year, month - 1, 1)
                        val currentMaxDate = today.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (currentInputDate > currentMaxDate) {
                            // 입력된 값이 최대 일수를 초과하면 최대 일수로 설정
                            currentInputDate = currentMaxDate
                            mBinding.date.setText(currentMaxDate.toString())
                            mBinding.date.setSelection(mBinding.date.text.length)
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    fun initView(isDate: Boolean, currentDate: String? = null) {
        setCancelable(false)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        if (isDate) {
            mBinding.layoutDate.visibility = View.GONE
        }

        if(currentDate != null) {
            if(currentDate.contains("-")) { // 일이 없을 경우
                val splitStr = currentDate.split("-")
                Log.d("splitStr", "splitStr ===> $splitStr")
                setDate(splitStr[0].toInt(), splitStr[1].toInt(), 0)
            } else { // 일이 있을 경우
                val splitStr = currentDate.split("/")
                Log.d("splitStr", "splitStr ===> $splitStr")
                setDate(splitStr[0].toInt(), splitStr[1].toInt(), splitStr[2].toInt())
            }
        }

        updateDate()

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
            if (month > 12) {
                month = 1 // 12월을 넘어가면 1월로 설정
                year += 1 // 연도도 증가
            }
            updateDate()
        }

        mBinding.btnMinusMonth.setOnClickListener {
            month -= 1
            if (month < 1) {
                month = 12 // 1월 미만이면 12월로 설정
                year -= 1 // 연도도 감소
            }
            updateDate()
        }

        // 일 조절 버튼
        mBinding.btnAddDate.setOnClickListener {
            date += 1
            updateDate()
        }

        mBinding.btnMinusDate.setOnClickListener {
            date -= 1
            if (date < 1) {
                date = 1 // 1일 미만이면 1일로 설정

            }
            updateDate()
        }

        //  취소 버튼 클릭 시
        mBinding.cancelBtn.setOnClickListener {
            dismiss()
        }

        //  완료 버튼 클릭 시
        mBinding.confirmBtn.setOnClickListener {
            if(month > 12) {
                showNoticePopup(mContext.getString(R.string.dateErr))
            } else if(date > 31) {
                showNoticePopup(mContext.getString(R.string.dateErr))
            } else {
                val selectedMonth = month.toString().padStart(2, '0')
                val selectedDate = date.toString().padStart(2, '0')
                if (isDate) {
                    onDateSelect?.invoke("$year-$selectedMonth")
                } else {
                    // 선택된 날짜
                    val selectedMonthDate ="$year$selectedMonth$selectedDate"

                    // 날짜 형태 바꿔주기
                    val dateFormat = SimpleDateFormat("yyyyMMdd")
                    val inputDateFormat = SimpleDateFormat("yyyy/MM/dd")

                    // 날짜 계산위해 캘린더 불러오기
                    val calendar = Calendar.getInstance()

                    // 현재 날짜
                    val today = dateFormat.format(calendar.time)
                    val inputToday = inputDateFormat.format(calendar.time)

                    // 오늘 날짜 기준으로 직전 한달 구하기
                    calendar.add(Calendar.MONTH, -1)
                    val previousMonthDate = dateFormat.format(calendar.time)
                    val inputPreviousMonthDate = inputDateFormat.format(calendar.time)

                    // 오늘 날짜 기준으로 하루 지난 날짜 구하기위해 현재 날짜를 문자열에서 날짜형태로 변환
                    val parsedArbitraryDate = dateFormat.parse(today)

                    // 오늘 날짜를 캘린더 날짜로 설정
                    calendar.time = parsedArbitraryDate!!

                    // 오늘 날짜에서 하루를 더해주기
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val nextDayDate = dateFormat.format(calendar.time)

                    if (!isDate) {

                        /*Log.d("test log", "selectedMonthDate >>> $selectedMonthDate")
                        Log.d("test log", "today >>>>>>>>>>>>>>> $today")
                        Log.d("test log", "previousMonthDate >>> $previousMonthDate")
                        Log.d("test log", "nextDayDate >>>>>>>>> $nextDayDate")*/

                        if (selectedMonthDate.toInt() < previousMonthDate.toInt() || selectedMonthDate.toInt() > nextDayDate.toInt()) {
                            showNoticePopup("오늘 기준으로 최대 한 달전의 전표만 조회가 가능합니다.")
                            /*if (isStartDate == true) {
                                onDateSelect?.invoke(inputPreviousMonthDate)
                            } else {
                                onDateSelect?.invoke(inputToday)
                            }*/
                        } else {
                            onDateSelect?.invoke("$year/$month/$date")
                            dismiss()
                        }
                    }
                }
                // dismiss()
            }
        }
    }

    private fun setDate(y: Int, m: Int, d: Int) {
        today.set(y, m, d)

        year = today.get(Calendar.YEAR)

        /**
         * date가 0인 경우 현재 달의 마지막 날의 하루 전 날짜를 의미하므로 0 ~ 11
         * date가 해당 달의 일수보다 큰 경우 Calendar는  자동으로 다음 달로 넘어가 처리함
         * 그래서 +1을 할 필요가 없음
         */
        month = if(currentDate!!.contains("/")) {
            today.get(Calendar.MONTH)
        } else {
            today.get(Calendar.MONTH) + 1
        }
        date = if (isStartDate == true) {
            today.get(Calendar.DATE-7)
        } else {
            today.get(Calendar.DATE)
        }
    }

    private fun showNoticePopup(msg: String) {
        val popupNotice = PopupNotice(mContext, msg)
        popupNotice.show()
    }
}