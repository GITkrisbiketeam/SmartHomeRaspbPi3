package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.krisbiketeam.smarthomeraspbpi3.R
import timber.log.Timber

enum class SpinnerType {
    DEFAULT,
    ENTRIES_USED,
    WITH_EMPTY
}

class SpinnerAdapter(context: Context, val type: SpinnerType, val entries: List<Any>) :
        ArrayAdapter<Any>(context, android.R.layout.simple_spinner_item, android.R.id.text1,
                entries.toMutableList().apply {
                    when {
                        all { it is String } -> add("")
                        all { it is Int } -> add(Int.MIN_VALUE)
                        all { it is Pair<*, *> } -> add(Pair("", false))
                    }
                }) {

    init {
        when (type) {
            SpinnerType.ENTRIES_USED -> setDropDownViewResource(R.layout.spinner_custom_item)
            else -> setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    override fun getCount() = super.getCount() - if (SpinnerType.WITH_EMPTY == type) 0 else 1

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            if (SpinnerType.ENTRIES_USED == type) {
                val iconView: AppCompatImageView? = findViewById(R.id.icon1)
                val textView: TextView? = findViewById(android.R.id.text1)
                //Timber.d("getDropDownView pos: $position iconView: $iconView")
                textView?.text = (getItem(position) as Pair<String, *>).first
                iconView?.visibility = if ((getItem(position) as Pair<*, Boolean>).second == true) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            if (SpinnerType.ENTRIES_USED == type) {
                val textView: TextView? = findViewById(android.R.id.text1)
                Timber.d("getView pos: $position textView: $textView")
                textView?.text = (getItem(position) as Pair<String, *>).first
            }
        }
    }

    override fun getPosition(item: Any?): Int {
        Timber.d("getPosition item: $item")
        if (entries.all { it is Pair<*, *> }) {
            for((index, any) in entries.withIndex()){
                if (item == (any as Pair<*, *>).first) return index
            }
            return -1
        } else {
            return super.getPosition(item)
        }
    }
}