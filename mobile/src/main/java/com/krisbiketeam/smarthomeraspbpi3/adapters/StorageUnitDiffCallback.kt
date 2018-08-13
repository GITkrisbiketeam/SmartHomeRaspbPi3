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

import android.support.v7.util.DiffUtil
import com.krisbiketeam.data.storage.dto.StorageUnit

class StorageUnitDiffCallback : DiffUtil.ItemCallback<StorageUnit<out Any>>() {

    override fun areItemsTheSame(oldItem: StorageUnit<out Any>, newItem: StorageUnit<out Any>): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: StorageUnit<out Any>, newItem: StorageUnit<out Any>): Boolean {
        return oldItem == newItem
    }
}