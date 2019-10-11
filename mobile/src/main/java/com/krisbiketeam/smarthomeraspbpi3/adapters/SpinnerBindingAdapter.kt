package com.krisbiketeam.smarthomeraspbpi3.adapters

class SpinnerBindingAdapter {
    /*companion object {
        @BindingAdapter("currentPosition")
        @JvmStatic
        fun setCurrentPosition(spinner: AppCompatSpinner, spinnerPosition: MutableLiveData<Int>) {
            spinnerPosition.value?.let {position ->
                //don't forget to break possible infinite loops!
                if (spinner.selectedItemPosition != position) {
                    spinner.setSelection(position, true)
                }
            }
        }

        @InverseBindingAdapter(attribute = "currentPosition", event =  "currentPositionAttrChanged")
        @JvmStatic
        fun getCurrentPosition(spinner: AppCompatSpinner) = spinner.selectedItemPosition

        @BindingAdapter("app:currentPositionAttrChanged")
        @JvmStatic
        fun setListeners(
                spinner: AppCompatSpinner,
                attrChange: InverseBindingListener
        ) {
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    attrChange.onChange()
                }

            }
        }
    }*/
}