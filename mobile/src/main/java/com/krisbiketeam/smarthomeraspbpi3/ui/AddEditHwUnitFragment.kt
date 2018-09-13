package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentAddEditHwUnitBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.AddEditHwUnitViewModel
import org.koin.android.architecture.ext.viewModel
import timber.log.Timber

class AddEditHwUnitFragment : Fragment() {
    private val addEditHwUnitViewModel by viewModel<AddEditHwUnitViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val hwUnitName = AddEditHwUnitFragmentArgs.fromBundle(arguments).hwUnitName
        val binding = DataBindingUtil.inflate<FragmentAddEditHwUnitBinding>(
                inflater, R.layout.fragment_add_edit_hw_unit, container, false).apply {
            viewModel = addEditHwUnitViewModel
            setLifecycleOwner(this@AddEditHwUnitFragment)
        }

        addEditHwUnitViewModel.homeUnitType.observe(viewLifecycleOwner, Observer { tableName ->
            Timber.d("unitType changed: $tableName")
        })
        addEditHwUnitViewModel.homeUnitListLiveData.observe(viewLifecycleOwner, Observer { tableName ->
            Timber.d("homeUnitListLiveData changed: $tableName")
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_add_edit_hw_unit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_save -> {
                Timber.d("action_save: ${addEditHwUnitViewModel.homeUnitListLiveData.value.toString()}")
                Timber.d("action_save: ${addEditHwUnitViewModel.name.value.toString()}")
                if (addEditHwUnitViewModel.homeUnitListLiveData.value?.
                                contains(addEditHwUnitViewModel.name.value) == true) {

                    //This name is already used
                    Timber.d("This name is already used")

                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
