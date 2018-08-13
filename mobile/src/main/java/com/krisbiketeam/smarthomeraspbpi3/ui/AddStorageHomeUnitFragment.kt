package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.AddStorageHomeUnitViewModel
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentAddStorageHomeUnitBinding

class AddStorageHomeUnitFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val addStorageHomeUnitViewModel = ViewModelProviders.of(this)
                .get(AddStorageHomeUnitViewModel::class.java)

        val binding = DataBindingUtil.inflate<FragmentAddStorageHomeUnitBinding>(
                inflater, R.layout.fragment_add_storage_home_unit, container, false).apply {
            viewModel = addStorageHomeUnitViewModel
            setLifecycleOwner(this@AddStorageHomeUnitFragment)

        }

        /*addStorageHomeUnitViewModel.name.observe(this, Observer { room ->
            shareText = if (room == null) {
                ""
            } else {
                getString(R.string.share_text_plant, room.name)
            }
        })*/

        setHasOptionsMenu(true)

        return binding.root
    }

    /*override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_room_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            *//*R.id.action_share -> {
                val shareIntent = ShareCompat.IntentBuilder.from(activity)
                        .setText(shareText)
                        .setType("text/plain")
                        .createChooserIntent()
                        .apply {
                            // https://android-developers.googleblog.com/2012/02/share-with-intents.html
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                // If we're on Lollipop, we can open the intent as a document
                                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            } else {
                                // Else, we will use the old CLEAR_WHEN_TASK_RESET flag
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                            }
                        }
                startActivity(shareIntent)
                return true
            }*//*
            else -> super.onOptionsItemSelected(item)
        }
    }*/
}
