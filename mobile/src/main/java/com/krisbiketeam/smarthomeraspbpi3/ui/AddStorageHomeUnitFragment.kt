package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentAddStorageHomeUnitBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.AddStorageHomeUnitViewModel
import org.koin.android.architecture.ext.viewModel
import timber.log.Timber

class AddStorageHomeUnitFragment : Fragment() {
    private val addStorageHomeUnitViewModel by viewModel<AddStorageHomeUnitViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentAddStorageHomeUnitBinding>(
                inflater, R.layout.fragment_add_storage_home_unit, container, false).apply {
            viewModel = addStorageHomeUnitViewModel
            setLifecycleOwner(this@AddStorageHomeUnitFragment)
        }

        addStorageHomeUnitViewModel.firebaseTableName.observe(viewLifecycleOwner, Observer { tableName ->
            Timber.d("firebaseTableName changed: $tableName")
            tableName?.let{
                addStorageHomeUnitViewModel.storageUnitListLiveData = FirebaseHomeInformationRepository.storageUnitListLiveData(tableName)
            }
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_room_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_save -> {
                Timber.d("action_save: ${addStorageHomeUnitViewModel.storageUnitListLiveData?.value.toString()}")

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
