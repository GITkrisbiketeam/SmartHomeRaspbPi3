package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.transition.Fade
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentStorageUnitDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.di.Params
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.StorageUnitDetailViewModel
import org.koin.android.architecture.ext.getViewModel
import org.koin.android.ext.android.setProperty
import timber.log.Timber

class StorageUnitDetailFragment : Fragment() {
    private lateinit var storageUnitDetailViewModel: StorageUnitDetailViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // set ROOM_NAME property fo Koin injection
        setProperty(Params.ROOM_NAME, StorageUnitDetailFragmentArgs.fromBundle(arguments).roomName)
        setProperty(Params.STORAGE_UNIT_NAME, StorageUnitDetailFragmentArgs.fromBundle(arguments).storageUnitName)
        setProperty(Params.STORAGE_UNIT_TYPE, StorageUnitDetailFragmentArgs.fromBundle(arguments).storageUnitType)
        storageUnitDetailViewModel = getViewModel()

        val binding: FragmentStorageUnitDetailBinding = DataBindingUtil.inflate<FragmentStorageUnitDetailBinding>(
                inflater, R.layout.fragment_storage_unit_detail, container, false).apply {
            viewModel = storageUnitDetailViewModel
            setLifecycleOwner(this@StorageUnitDetailFragment)
        }

        storageUnitDetailViewModel.isEditMode.observe(viewLifecycleOwner, Observer { isEditMode ->
            // in Edit Mode we need to listen for storageUnitNameList, as there is no reference in xml layout to trigger its observer, but can we find some better way
            if (isEditMode == true) {
                storageUnitDetailViewModel.storageUnitNameList.observe(viewLifecycleOwner, Observer {  })
            } else {
                storageUnitDetailViewModel.storageUnitNameList.removeObservers(viewLifecycleOwner)
            }
            activity?.invalidateOptionsMenu()
            // Animate Layout edit mode change
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade())
        })
        storageUnitDetailViewModel.unitTaskList.observe(viewLifecycleOwner, Observer { taskList ->
            taskList?.let {
                // Update UnitTask list
                storageUnitDetailViewModel.unitTaskListAdapter.submitList(it)
            }
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_storage_unit_details_add_edit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        when (storageUnitDetailViewModel.isEditMode.value) {
            true -> {
                menu?.findItem((R.id.action_discard))?.isVisible = true
                menu?.findItem((R.id.action_save))?.isVisible = true
                menu?.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu?.findItem((R.id.action_discard))?.isVisible = false
                menu?.findItem((R.id.action_save))?.isVisible = false
                menu?.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${storageUnitDetailViewModel.isEditMode}")
                storageUnitDetailViewModel.isEditMode.value = true
                return true
            }
            R.id.action_save -> {
                Timber.d("action_save: ${storageUnitDetailViewModel.storageUnitNameList.value.toString()}")
                Timber.d("action_save: ${storageUnitDetailViewModel.name.value.toString()}")
                if (storageUnitDetailViewModel.storageUnitNameList.value?.contains(storageUnitDetailViewModel.name.value) == true) {

                    //This name is already used
                    Timber.d("This name is already used")

                } else {
                    // navigate back Up from this Fragment
                    findNavController().navigateUp()
                }
                return true
            }
            R.id.action_discard -> {
                storageUnitDetailViewModel.isEditMode.value = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
