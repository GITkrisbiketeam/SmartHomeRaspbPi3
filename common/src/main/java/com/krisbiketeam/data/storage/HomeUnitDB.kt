/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krisbiketeam.data.storage

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import java.util.*

/**
 * Represents a button press event
 */
@IgnoreExtraProperties
data class HomeUnitDB(var name: String, var connectionType: ConnectionType,
                      var location: String, var pinName: String,
                      var softAddress: Int? = null, var value: Any? = null,
                      var localtime: Long = Date().time,
                      var servertime: Map<String, String>? = ServerValue.TIMESTAMP)
