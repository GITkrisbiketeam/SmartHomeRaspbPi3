package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.databinding.InverseBindingListener
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import com.krisbiketeam.data.MyLiveDataState
import timber.log.Timber


/*@BindingAdapter("imageFromUrl")
fun imageFromUrl(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        Glide.with(view.context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view)
    }
}*/

@BindingAdapter("showIf")
fun showIf(view: FloatingActionButton, isShow: Boolean?) {
    if (isShow == null || isShow) view.show() else view.hide()
}

@BindingAdapter("stateBasedVisibility")
fun stateBasedVisibility(view: View, pair: Pair<MyLiveDataState, Any>?) {
    Timber.d("stateBasedVisibility pair: $pair; view: $view")
    pair?.let {
        when (it.first) {
            MyLiveDataState.CONNECTING -> {
                view.visibility = if (view is ProgressBar) View.VISIBLE else View.GONE
            }
            MyLiveDataState.INIT,
            MyLiveDataState.ERROR,
            MyLiveDataState.DONE -> {
                view.visibility = if (view is ProgressBar) View.GONE else View.VISIBLE
            }
        }
    }
}

@BindingAdapter("addDivider")
fun addDivider(recyclerView: RecyclerView, add: Boolean?) {
    Timber.d("addDivider recyclerView: $recyclerView; view: $add")
    if (add == null || add) recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
}

@BindingAdapter("entries")
fun bindEntriesData(spinner: AppCompatSpinner, entries: List<Any>?) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entries: $entries tag: ${spinner.tag}")
    if (entries != null) {
        //Add empty first element to list
        //Add empty element to list to  be able to show blank not selected item
        object : ArrayAdapter<Any>(spinner.context,
                android.R.layout.simple_spinner_item,
                entries.toMutableList().apply {
                    add("")
                }) {
                    override fun getCount() = super.getCount() - 1
                }.apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = this
                    val pos = getPosition(spinner.tag)
                    Timber.d("bindEntriesData pos: $pos spinner.tag: ${spinner.tag}")
                    if (pos in 0 .. spinner.count) {
                        spinner.setSelection(pos, false)
                    } else {
                        spinner.setSelection(spinner.count, false)
                    }
                }
    }
}

@BindingAdapter("entriesWithEmpty")
fun bindEntriesWithEmptyData(spinner: AppCompatSpinner, entries: List<Any>?) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("entriesWithEmpty entries: $entries tag: ${spinner.tag}")
    if (entries != null) {
        //Add empty first element to list
        //Add empty element to list to  be able to show blank not selected item
        ArrayAdapter<Any>(spinner.context,
                android.R.layout.simple_spinner_item,
                entries.toMutableList().apply {
                    add("")
                })
            .apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = this
                val pos = getPosition(spinner.tag)
                Timber.d("entriesWithEmpty pos: $pos")
                if (pos in 0 .. spinner.count-1) {
                    spinner.setSelection(pos, false)
                } else {
                    spinner.setSelection(spinner.count-1, false)
                }
            }
    }
}

@BindingAdapter("selectedValue", "selectedValueAttrChanged", requireAll=false)
fun bindSpinnerData(spinner: AppCompatSpinner, newSelectedValue: String?, newTextAttrChanged: InverseBindingListener) {
    Timber.d("selectedValue BindingAdapter newSelectedValue: $newSelectedValue")
    spinner.apply {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Timber.d("selectedValue $newSelectedValue onItemSelected : $position")
                newTextAttrChanged.onChange()
            }
        }
        tag = newSelectedValue?.let{it} ?: ""
        // This is for static entries list
        if (newSelectedValue != null && adapter != null) {
            Timber.d("selectedValue $newSelectedValue count : ${adapter.count}")
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i) == newSelectedValue) {
                    setSelection(i, false)
                    break
                }
            }
        }
    }
}

@InverseBindingAdapter(attribute = "selectedValue", event = "selectedValueAttrChanged")
fun captureSelectedValue(spinner: AppCompatSpinner): String? {
    Timber.d("selectedValue InverseBindingAdapter ${spinner.selectedItem as String}")
    return (spinner.selectedItem as String).run {
        if(isEmpty()) null else this
    }
}

@BindingAdapter("setChecked")
fun bindSetChecked(switch: SwitchCompat, value: Any?) {
    Timber.d("setSwitch BindingAdapter value: $value")
    if (value is Boolean) {
        switch.setChecked(value)
    }
}

/*@BindingAdapter("setChecked", "setCheckedAttrChanged", requireAll=false)
fun bindSetChecked(switch: SwitchCompat, value: Any?, attrChanged: InverseBindingListener) {
    Timber.d("setSwitch BindingAdapter value: $value attrChanged: $attrChanged")
    *//*switch.setOnCheckedChangeListener { buttonView, isChecked ->
        Timber.d("setSwitch isChecked : $isChecked")
        attrChanged.onChange()
    }*//*
    if (value is Boolean) {
        switch.setChecked(value)
    }
}


@InverseBindingAdapter(attribute = "setChecked", event = "setCheckedAttrChanged")
fun inversebindSetChecked(switch: SwitchCompat): Any? {
    Timber.d("setSwitch InverseBindingAdapter ${switch.isChecked}")
    return switch.isChecked
}*/
