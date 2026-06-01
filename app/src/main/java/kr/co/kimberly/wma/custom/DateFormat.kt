package kr.co.kimberly.wma.custom

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.text.SimpleDateFormat
import java.util.*

class DateFormat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())

    init {
        addTextChangedListener(object : TextWatcher {
            private var current = ""
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val input = s.toString().replace("-", "")
                if (input.length == 8) {
                    val formatted = formatDate(input)
                    isUpdating = true
                    setText(formatted)
                    setSelection(formatted.length)
                    isUpdating = false
                }
            }
        })

        onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = text.toString().replace("-", "")
                if (input.length == 6) {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString().substring(0, 2)
                    val formatted = formatDate("$currentYear$input")
                    setText(formatted)
                }
            }
        }
    }

    private fun formatDate(input: String): String {
        return try {
            val parsedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(input)
            dateFormat.format(parsedDate)
        } catch (e: Exception) {
            input
        }
    }
}
