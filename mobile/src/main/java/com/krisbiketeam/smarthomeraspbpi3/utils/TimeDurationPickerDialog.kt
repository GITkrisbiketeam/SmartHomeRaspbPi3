package com.krisbiketeam.smarthomeraspbpi3.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.WidgetTimeDurationPickerBinding

private const val HOUR = "hour"
private const val MINUTE = "minute"
private const val SECONDS = "seconds"

class TimeDurationPicker(context: Context, private val mTimeSetListener: (Int, Int, Int) -> Unit, private val initialHours: Int, private val initialMinutes: Int, private val initialSeconds: Int) :
        AlertDialog(context), DialogInterface.OnClickListener {

    private val rootBinding: WidgetTimeDurationPickerBinding = WidgetTimeDurationPickerBinding.inflate(LayoutInflater.from(getContext()))

    init {
        setView(rootBinding.root)
        setButton(BUTTON_POSITIVE, getContext().getString(R.string.ok), this)
        setButton(BUTTON_NEGATIVE, getContext().getString(R.string.cancel), this)

        rootBinding.dialogNumberPickerHours.apply {
            maxValue = 24
            minValue = 0
            value = initialHours
        }
        rootBinding.dialogNumberPickerMinutes.apply {
            maxValue = 59
            minValue = 0
            value = initialMinutes
            setOnValueChangedListener { picker, oldVal, newVal ->
                if (oldVal == 59 && newVal == 0) {
                    rootBinding.dialogNumberPickerHours.value++
                } else if (oldVal == 0 && newVal == 59) {
                    rootBinding.dialogNumberPickerHours.value--
                }
            }
        }
        rootBinding.dialogNumberPickerSeconds.apply {
            maxValue = 59
            minValue = 0
            value = initialSeconds
            setOnValueChangedListener { picker, oldVal, newVal ->
                if (oldVal == 59 && newVal == 0) {
                    rootBinding.dialogNumberPickerMinutes.value++
                } else if (oldVal == 0 && newVal == 59) {
                    rootBinding.dialogNumberPickerMinutes.value--
                }
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            BUTTON_POSITIVE -> mTimeSetListener(rootBinding.dialogNumberPickerHours.value,
                    rootBinding.dialogNumberPickerMinutes.value, rootBinding.dialogNumberPickerSeconds.value)
            BUTTON_NEGATIVE -> cancel()
        }
    }

    override fun onSaveInstanceState(): Bundle {
        val state = super.onSaveInstanceState()
        state.putInt(HOUR, rootBinding.dialogNumberPickerHours.value)
        state.putInt(MINUTE, rootBinding.dialogNumberPickerMinutes.value)
        state.putInt(SECONDS, rootBinding.dialogNumberPickerSeconds.value)
        return state
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val hour = savedInstanceState.getInt(HOUR)
        val minute = savedInstanceState.getInt(MINUTE)
        val seconds = savedInstanceState.getInt(SECONDS)
        rootBinding.dialogNumberPickerHours.value = hour
        rootBinding.dialogNumberPickerMinutes.value = minute
        rootBinding.dialogNumberPickerSeconds.value = seconds
    }
}