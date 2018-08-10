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

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krisbiketeam.data.storage.dto.Room
import timber.log.Timber
import com.krisbiketeam.smarthomeraspbpi3.databinding.ListItemRoomBinding

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class RoomAdapter : ListAdapter<Room, RoomAdapter.ViewHolder>(RoomDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = getItem(position)
        holder.apply {
            bind(createOnClickListener(plant.name), plant)
            itemView.tag = plant
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ListItemRoomBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(name: String): View.OnClickListener {
        return View.OnClickListener {
            Timber.d("onClick")
            /*val direction = PlantListFragmentDirections.ActionPlantListFragmentToPlantDetailFragment(name)
            it.findNavController().navigate(direction)*/
        }
    }

    class ViewHolder(
        private val binding: ListItemRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: Room) {
            binding.apply {
                clickListener = listener
                room = item
                executePendingBindings()
            }
        }
    }
}