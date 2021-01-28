package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.Group
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialAutoCompleteTextView
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
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

@BindingAdapter("hideIfEmpty")
fun hideIfEmpty(view: Group, type: String?) {
    view.visibility = if (TextUtils.isEmpty(type)) View.GONE else View.VISIBLE
}

@BindingAdapter("stateBasedVisibility")
fun stateBasedVisibility(view: View, pair: Pair<MyLiveDataState, Any>?) {
    Timber.d("stateBasedVisibility pair: $pair; view: $view")
    pair?.let {
        when (it.first) {
            MyLiveDataState.CONNECTING                                        -> {
                view.visibility = if (view is ProgressBar) View.VISIBLE else View.GONE
            }
            MyLiveDataState.INIT, MyLiveDataState.ERROR, MyLiveDataState.DONE -> {
                view.visibility = if (view is ProgressBar) View.GONE else View.VISIBLE
            }
        }
    }
}

@BindingAdapter("addDivider")
fun addDivider(recyclerView: RecyclerView, add: Boolean?) {
    Timber.d("addDivider recyclerView: $recyclerView; view: $add")
    if (add == null || add) recyclerView.addItemDecoration(
            DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
}

@BindingAdapter("setChecked")
fun bindSetChecked(switch: SwitchCompat, value: Any?) {
    Timber.d("setSwitch BindingAdapter value: $value")
    if (value is Boolean) {
        switch.isChecked = value
    }
}

@BindingAdapter("endIconDropDownEnabled")
fun bindEndIconDropDownEnabled(textInputLayout: TextInputLayout, enabled: Boolean? = true) {
    Timber.d("endIconDropDownEnabled BindingAdapter enabled: $enabled")
    if (enabled is Boolean) {
        if(enabled) {
            textInputLayout.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        } else {
            textInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }
}

@BindingAdapter("valueAttrChanged")
fun MaterialAutoCompleteTextView.setListener(listener: InverseBindingListener?) {
    this.onItemClickListener = if (listener != null) {
        AdapterView.OnItemClickListener { _, _, position, _ ->
            (adapter as AutoCompleteAdapter?)?.position = position
            //listSelection = position
            listener.onChange()
        }
    } else {
        null
    }
}

@get:InverseBindingAdapter(attribute = "value")
@set:BindingAdapter("value")
var MaterialAutoCompleteTextView.selectedValue: Any?
    get() {
        return if (listSelection != ListView.INVALID_POSITION) {
            adapter?.getItem(listSelection)
        } else {
            val position = (adapter as AutoCompleteAdapter?)?.position ?: ListView.INVALID_POSITION
            if (position != ListView.INVALID_POSITION) {
                val item = adapter?.getItem(position)
                if (item is Pair<*, *>) {
                    item.first
                } else {
                    item
                }
            } else {
                null
            }
        }
    }
    set(value) {
        val newValue =
                value ?: if (adapter?.count?: ListView.INVALID_POSITION > 0) adapter?.getItem(0) else null
        // Disable filtering so that we can select different item from already preselected one
        setText(newValue?.toString(), (adapter as AutoCompleteAdapter?)?.filterable ?: false)
        (adapter as AutoCompleteAdapter?)?.let {
            if (it.count > 0) {
                val position = it.getPosition(newValue)
                listSelection = position
            }
        }
    }

// entriesFilterable does not really work as expected
@BindingAdapter("entriesAutoComplete", "entriesFilterable", requireAll = false)
fun MaterialAutoCompleteTextView.setItems(entries: List<Any>?, filterable: Boolean = false) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entriesAutoComplete: $entries tag: $tag")
    if (entries != null) {
        setAdapter(
                AutoCompleteAdapter(context, AutoCompleteAdapterType.DEFAULT, entries, filterable))
    }
}

@BindingAdapter("entriesUsedAutoComplete", "entriesFilterable", requireAll = false)
fun MaterialAutoCompleteTextView.setItemsUsed(entries: List<Any>?, filterable: Boolean = false) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entriesUsedAutoComplete: $entries tag: $tag")
    if (entries != null) {
        setAdapter(AutoCompleteAdapter(context, AutoCompleteAdapterType.ENTRIES_USED, entries,
                                       filterable))
    }
}

@BindingAdapter("entriesWithEmptyAutoComplete", "entriesFilterable", requireAll = false)
fun MaterialAutoCompleteTextView.setItemsWithEmpty(entries: List<Any>?,
                                                   filterable: Boolean = false) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entriesWithEmptyAutoComplete: $entries tag: $tag")
    if (entries != null) {
        setAdapter(AutoCompleteAdapter(context, AutoCompleteAdapterType.WITH_EMPTY, entries,
                                       filterable))
    }
}