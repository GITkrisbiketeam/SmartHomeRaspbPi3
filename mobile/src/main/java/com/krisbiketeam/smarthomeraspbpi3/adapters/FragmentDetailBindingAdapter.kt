package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
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

@BindingAdapter("visibility")
fun visibility(view: View, visible: Boolean?) {
    view.visibility = if (visible == true) View.VISIBLE else View.GONE
}

@BindingAdapter("stateBasedVisibility")
fun stateBasedVisibility(view: View, pair: Pair<MyLiveDataState, Any>?) {
    Timber.d("stateBasedVisibility pair: $pair; view: $view")
    pair?.let {
        when (it.first) {
            MyLiveDataState.CONNECTING -> {
                view.visibility = if (view is ProgressBar) View.VISIBLE else View.GONE
            }

            MyLiveDataState.INIT, MyLiveDataState.ERROR, MyLiveDataState.DONE -> {
                view.visibility = if (view is ProgressBar) View.GONE else View.VISIBLE
            }
        }
    }
}

@BindingAdapter("bleStateBasedVisibility")
fun bleStateBasedVisibility(view: View, state: MyLiveDataState) {
    Timber.d("stateBasedVisibility state: $state; view: $view")
    when (state) {
        MyLiveDataState.CONNECTING -> {
            view.visibility = if (view is ProgressBar) View.VISIBLE else View.GONE
        }

        MyLiveDataState.INIT, MyLiveDataState.ERROR, MyLiveDataState.DONE -> {
            view.visibility = if (view is ProgressBar) View.GONE else View.VISIBLE
        }

        else -> {
            view.visibility = if (view is ProgressBar) View.GONE else View.VISIBLE
        }
    }
}

@BindingAdapter("addDivider")
fun addDivider(recyclerView: RecyclerView, add: Boolean?) {
    Timber.d("addDivider recyclerView: $recyclerView; view: $add")
    if (add == null || add) recyclerView.addItemDecoration(
        DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
    )
}

@BindingAdapter("setChecked")
fun bindSetChecked(switch: SwitchCompat, value: Any?) {
    Timber.d("setSwitch BindingAdapter value: $value")
    if (value is Boolean) {
        switch.isChecked = value
    } else {
        switch.isChecked = true.toString() == value
    }
}

@BindingAdapter("endIconDropDownEnabled")
fun bindEndIconDropDownEnabled(textInputLayout: TextInputLayout, enabled: Boolean? = true) {
    Timber.d("endIconDropDownEnabled BindingAdapter enabled: $enabled")
    if (enabled is Boolean) {
        if (enabled) {
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
                value
                        ?: if (adapter?.count ?: ListView.INVALID_POSITION > 0) adapter?.getItem(0) else null
        // Disable filtering so that we can select different item from already preselected one
        setText(newValue?.toString(), false)
        (adapter as AutoCompleteAdapter?)?.let {
            if (it.count > 0) {
                val position = it.getPosition(newValue)
                listSelection = position
            }
        }
    }

@BindingAdapter("entriesAutoComplete", "entriesWithEmpty", "entriesUsed", requireAll = false)
fun MaterialAutoCompleteTextView.setItems(entries: List<Any>?, withEmpty: Boolean = false, entriesUsed:Boolean = false) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entriesAutoComplete: $entries tag: $tag")
    if (entries != null) {
        setAdapter(AutoCompleteAdapter(context, entries, entriesUsed,  withEmpty))
    }
}

@BindingAdapter("entries")
fun MultiAutoCompleteTextView.setItems(entries: List<Any>?) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesData entriesAutoComplete: $entries tag: $tag")
    if (entries != null) {
        setAdapter(AutoCompleteAdapter(context, entries, false,  false))
    }
}

@BindingAdapter("defaultComaTokenizer")
fun defaultComaTokenizer(view: MultiAutoCompleteTextView, add: Boolean?) {
    Timber.d("defaultComaTokenizer $add")
    if (add == true) {
        view.setTokenizer(object : MultiAutoCompleteTextView.CommaTokenizer(){
            override fun terminateToken(text: CharSequence): CharSequence {
                return if(view.text.contains(text, ignoreCase = true)){
                    // selecting the same item will remove it so return empty text and remove this text from MultiAutoCompleteTextView in separate thread
                    view.post {
                        val startIdx = view.text.indexOf(text.toString(), 0, true)
                        view.text = view.text.replace(startIdx, kotlin.math.min(startIdx + text.length + 2, view.text.length), "")
                    }
                    ""
                } else {
                    super.terminateToken(text)
                }
            }
        })
    }
}