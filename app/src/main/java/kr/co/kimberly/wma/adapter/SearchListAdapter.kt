package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListPopupWindow
import kr.co.kimberly.wma.databinding.CellSearchBinding

class CustomAutoCompleteAdapter(context: Context, private val itemList: List<String>) :
    ArrayAdapter<String>(context, 0, itemList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: CellSearchBinding
        val view: View

        if (convertView == null) {
            binding = CellSearchBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            binding = convertView.tag as CellSearchBinding
            view = convertView
        }

        val item = getItem(position)
        binding.itemName.text = item

        return view
    }

    @SuppressLint("DiscouragedPrivateApi")
    fun setAutoCompleteDropDownHeight(autoCompleteTextView: AutoCompleteTextView, visibleItemCount: Int) {
        autoCompleteTextView.post {
            try {
                val popup = AutoCompleteTextView::class.java.getDeclaredField("mPopup")
                popup.isAccessible = true
                val listPopupWindow = popup.get(autoCompleteTextView) as ListPopupWindow
                val itemHeight = 80
                listPopupWindow.height = itemHeight * visibleItemCount
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}