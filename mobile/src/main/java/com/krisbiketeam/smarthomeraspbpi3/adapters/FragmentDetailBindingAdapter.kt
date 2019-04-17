package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.text.TextUtils
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.Group
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
        spinner.apply {
            val spinAdapter = SpinnerAdapter(spinner.context, SpinnerType.DEFAULT, entries)
            adapter = spinAdapter
        }
    }
}

@BindingAdapter("entriesUsed")
fun bindEntriesUsedData(spinner: AppCompatSpinner, entries: List<Pair<String, Boolean>>?) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesUsedData entries: $entries tag: ${spinner.tag}")
    if (entries != null) {
        spinner.apply {
            val spinAdapter = SpinnerAdapter(spinner.context, SpinnerType.ENTRIES_USED, entries)
            adapter = spinAdapter
        }
    }
}

@BindingAdapter("entriesWithEmpty")
fun bindEntriesWithEmptyData(spinner: AppCompatSpinner, entries: List<Any>?) {
    // This is for dynamic entries list, like form ViewModel LiveData
    Timber.d("bindEntriesWithEmptyData entries: $entries tag: ${spinner.tag}")
    if (entries != null) {
        spinner.apply {
            val spinAdapter = SpinnerAdapter(spinner.context, SpinnerType.WITH_EMPTY, entries)
            adapter = spinAdapter
        }
    }
}

@BindingAdapter("setChecked")
fun bindSetChecked(switch: SwitchCompat, value: Any?) {
    Timber.d("setSwitch BindingAdapter value: $value")
    if (value is Boolean) {
        switch.isChecked = value
    }
}