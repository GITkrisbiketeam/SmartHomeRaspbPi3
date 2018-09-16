package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.Fade
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.view.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.di.Params
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitDetailViewModel
import org.koin.android.architecture.ext.getViewModel
import org.koin.android.ext.android.setProperty
import timber.log.Timber

class HomeUnitDetailFragment : Fragment() {
    private lateinit var homeUnitDetailViewModel: HomeUnitDetailViewModel
    private lateinit var rootBinding: FragmentHomeUnitDetailBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // set HOME_UNIT_NAME and TYPE property fo Koin injection
        setProperty(Params.HOME_UNIT_NAME, HomeUnitDetailFragmentArgs.fromBundle(arguments).homeUnitName)
        setProperty(Params.HOME_UNIT_TYPE, HomeUnitDetailFragmentArgs.fromBundle(arguments).homeUnitType)
        homeUnitDetailViewModel = getViewModel()

        rootBinding = DataBindingUtil.inflate<FragmentHomeUnitDetailBinding>(
                inflater, R.layout.fragment_home_unit_detail, container, false).apply {
            viewModel = homeUnitDetailViewModel
            setLifecycleOwner(this@HomeUnitDetailFragment)
        }

        homeUnitDetailViewModel.isEditMode.observe(viewLifecycleOwner, Observer { isEditMode ->
            // in Edit Mode we need to listen for homeUnitNameList, as there is no reference in xml layout to trigger its observer, but can we find some better way
            if (isEditMode == true) {
                homeUnitDetailViewModel.homeUnitNameList.observe(viewLifecycleOwner, Observer { })
            } else {
                homeUnitDetailViewModel.homeUnitNameList.removeObservers(viewLifecycleOwner)
            }
            activity?.invalidateOptionsMenu()
            // Animate Layout edit mode change
            TransitionManager.beginDelayedTransition(rootBinding.root as ViewGroup, Fade())
        })
        homeUnitDetailViewModel.unitTaskList.observe(viewLifecycleOwner, Observer { taskList ->
            taskList?.let {
                // Update UnitTask list
                homeUnitDetailViewModel.unitTaskListAdapter.submitList(it)
            }
        })

        setHasOptionsMenu(true)

        return rootBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_home_unit_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        when (homeUnitDetailViewModel.isEditMode.value) {
            true -> {
                menu?.findItem((R.id.action_discard))?.isVisible = true
                menu?.findItem((R.id.action_save))?.isVisible = true
                menu?.findItem((R.id.action_delete))?.isVisible = true
                menu?.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu?.findItem((R.id.action_discard))?.isVisible = false
                menu?.findItem((R.id.action_save))?.isVisible = false
                menu?.findItem((R.id.action_delete))?.isVisible = false
                menu?.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // If showing progress do not allow app bar actions
        if (homeUnitDetailViewModel.showProgress.value == true) {
            return false
        }
        return when (item?.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${homeUnitDetailViewModel.isEditMode}")
                homeUnitDetailViewModel.isEditMode.value = true
                return true
            }
            R.id.action_save -> {
                val (messageId, showDialog) = homeUnitDetailViewModel.trySaveChanges()
                Timber.d("action_save ${getString(messageId)}")
                if (messageId > 0) {
                    if (showDialog) {

                    } else {
                        Snackbar.make(rootBinding.root, messageId, Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    //hmm, this should not happen
                    // navigate back Up from this Fragment
                    //findNavController().navigateUp()
                }
                return true
            }
            R.id.action_discard -> {
                homeUnitDetailViewModel.isEditMode.value = false
                homeUnitDetailViewModel.discardChanges()
                return true
            }
            R.id.action_delete -> {
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
