/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.databinding.InverseBindingListener
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.AppCompatSpinner
import android.view.View
import android.widget.AdapterView
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

@BindingAdapter("selectedValue", "selectedValueAttrChanged", requireAll=false)
fun bindSpinnerData(spinner: AppCompatSpinner, newSelectedValue: String?, newTextAttrChanged: InverseBindingListener) {
    Timber.d("selectedValue BindingAdapter newSelectedValue: $newSelectedValue")
    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            Timber.d("selectedValue onNothingSelected")
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            Timber.d("selectedValue onItemSelected : $position")
            newTextAttrChanged.onChange()
        }
    }
}

@InverseBindingAdapter(attribute = "selectedValue", event = "selectedValueAttrChanged")
fun captureSelectedValue(spinner: AppCompatSpinner): String {
    Timber.d("selectedValue InverseBindingAdapter ${spinner.selectedItem as String}")
    return spinner.selectedItem as String
}
/*
@BindingAdapter("wateringText")
fun wateringText(textView: TextView, wateringInterval: Int) {
    val resources = textView.context.resources
    val quantityString = resources.getQuantityString(R.plurals.watering_needs_suffix,
        wateringInterval, wateringInterval)

    textView.text = SpannableStringBuilder()
        .bold { append(resources.getString(R.string.watering_needs_prefix)) }
        .append(" ")
        .italic { append(quantityString) }
}*/
