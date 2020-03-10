package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.krisbiketeam.smarthomeraspbpi3.R
import timber.log.Timber

enum class AutoCompleteAdapterType {
    DEFAULT,
    ENTRIES_USED,
    WITH_EMPTY
}

class AutoCompleteAdapter(context: Context, val type: AutoCompleteAdapterType, entries: List<Any>, val filterable: Boolean) :
        ArrayAdapter<Any>(context,
                          if (type == AutoCompleteAdapterType.ENTRIES_USED) R.layout.spinner_custom_item else android.R.layout.simple_spinner_dropdown_item,
                          android.R.id.text1, entries.toMutableList().also { Timber.d("SpinnerAdapter $it") }) {

    var position: Int = ListView.INVALID_POSITION

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            if (AutoCompleteAdapterType.ENTRIES_USED == type) {
                val iconView: AppCompatImageView? = findViewById(R.id.icon1)
                val textView: TextView? = findViewById(android.R.id.text1)
                //Timber.d("getDropDownView pos: $position iconView: $iconView")
                textView?.text = (getItem(position) as Pair<*, *>).first as String
                iconView?.visibility = if ((getItem(position) as Pair<*, *>).second as Boolean) View.VISIBLE else View.GONE
            }
        }
    }
}