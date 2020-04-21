package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.krisbiketeam.smarthomeraspbpi3.R

enum class AutoCompleteAdapterType {
    DEFAULT,
    ENTRIES_USED,
    WITH_EMPTY
}

class AutoCompleteAdapter(context: Context, val type: AutoCompleteAdapterType, entries: List<Any>,
                          val filterable: Boolean) :
        ArrayAdapter<Any>(context, if (type == AutoCompleteAdapterType.ENTRIES_USED) {
            R.layout.spinner_custom_item
        } else {
            android.R.layout.simple_spinner_dropdown_item
        }, android.R.id.text1, entries.toMutableList().apply {
            if (type == AutoCompleteAdapterType.WITH_EMPTY) {
                when {
                    all { it is String } -> add(0, "")
                    all { it is Int } -> add(0, "")
                    all { it is Pair<*, *> } -> add(0, Pair("", false))
                }
            }
        }) {

    var position: Int = ListView.INVALID_POSITION

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            if (AutoCompleteAdapterType.ENTRIES_USED == type) {
                val iconView: AppCompatImageView? = findViewById(R.id.icon1)
                val textView: TextView? = findViewById(android.R.id.text1)
                //Timber.d("getDropDownView pos: $position iconView: $iconView")
                textView?.text = (getItem(position) as Pair<*, *>).first as String
                iconView?.visibility =
                        if ((getItem(position) as Pair<*, *>).second as Boolean) View.VISIBLE else View.GONE
            }
        }
    }
}