package com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables

// region "users"
const val USER_INFORMATION_BASE = "users"

const val USER_NAME= "name"
const val USER_EMAIL = "email"
const val USER_NOTIFICATION_TOKENS = "notificationTokens"
// endregion

// region "notification"
const val NOTIFICATION_INFORMATION_BASE = "notification"
// endregion

// region "home"
const val HOME_INFORMATION_BASE = "home"

// region HW Units
const val HOME_HW_UNITS_BASE = "hw_units"

const val HW_ERROR_INFORMATION_BASE = "hw_error"
const val HW_RESTART_INFORMATION_BASE = "hw_restart"
// endregion

// region Rooms
const val HOME_ROOMS = "rooms"
// endregion

// region HomeUnits
const val HOME_UNITS_BASE = "home_units"

const val HOME_ACTUATORS = "actuators"
const val HOME_LIGHTS = "lights"
const val HOME_LIGHT_SWITCHES = "light_switches"
const val HOME_REED_SWITCHES = "reed_switches"
const val HOME_MOTIONS = "motions"
const val HOME_TEMPERATURES = "temperatures"
const val HOME_PRESSURES = "pressures"
const val HOME_HUMIDITY = "humidity"
const val HOME_BLINDS = "blinds"

// region unitTasks
const val HOME_UNIT_TASKS = "unitsTasks"
// endregion

// endregion

// region Preferences
const val HOME_PREFERENCES_BASE = "preferences"
// endregion

// region Online and last online time
const val HOME_ONLINE = "online"
const val HOME_LAST_ONLINE_TIME = "lastOnline"
// endregion

// region Logs
const val LOG_INFORMATION_BASE = "log"
// endregion

// region "home"
const val RESTART_APP = "restart_app"
const val RESTART_PI = "restart_pi"
const val RESTART_BOARD = "restart_board"
// endregion
// endregion