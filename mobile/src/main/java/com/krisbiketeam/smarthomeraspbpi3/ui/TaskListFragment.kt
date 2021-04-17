package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.TaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentTaskListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.TaskListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber


class TaskListFragment : Fragment() {

    private val taskListViewModel by viewModel<TaskListViewModel>()

    private val analytics: Analytics by inject()

    private val itemTouchHelper by lazy {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or
                DOWN or
                START or
                END, 0) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                taskListViewModel.moveItem(viewHolder.adapterPosition, target.adapterPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                  direction: Int) {
            }
        })
    }

    @ExperimentalCoroutinesApi
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val binding: FragmentTaskListBinding = DataBindingUtil.inflate<FragmentTaskListBinding>(
                inflater, R.layout.fragment_task_list, container, false).apply {
            viewModel = taskListViewModel
            lifecycleOwner = this@TaskListFragment
            fab.setOnClickListener {
                val direction = TaskListFragmentDirections.actionTaskListFragmentToHomeUnitDetailFragment("","","")
                findNavController().navigate(direction)
            }
            val adapter: TaskListAdapter by inject()
            taskList.layoutManager = GridLayoutManager(requireContext(), 2)
            taskList.adapter = adapter

            subscribeTaskList(adapter)
        }
        taskListViewModel.isEditMode.observe(viewLifecycleOwner, { editMode ->
            activity?.invalidateOptionsMenu()
            itemTouchHelper.attachToRecyclerView(if (editMode) binding.taskList else null)
        })
        setHasOptionsMenu(true)

        analytics.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_room_list, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (taskListViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_finish))?.isVisible = true
                menu.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu.findItem((R.id.action_finish))?.isVisible = false
                menu.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${taskListViewModel.isEditMode}")
                taskListViewModel.isEditMode.value = true
                return true
            }
            R.id.action_finish -> {
                taskListViewModel.isEditMode.value = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @ExperimentalCoroutinesApi
    private fun subscribeTaskList(adapter: TaskListAdapter) {
        taskListViewModel.taskListFromFlow.observe(viewLifecycleOwner,
                { taskUnitsList ->
                    Timber.d("subscribeUi taskUnitsList: $taskUnitsList")
                    adapter.submitList(taskUnitsList)
                })
    }
}