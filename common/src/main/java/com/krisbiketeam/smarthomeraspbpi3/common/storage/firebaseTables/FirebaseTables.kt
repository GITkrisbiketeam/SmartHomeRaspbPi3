package com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables

// region "users"
const val USER_INFORMATION_BASE = "users"

const val USER_NAME= "name"
const val USER_ONLINE= "online"
const val USER_EMAIL = "email"
const val USER_NOTIFICATION_TOKENS = "notificationTokens"
// endregion

// region "notification"
const val NOTIFICATION_INFORMATION_BASE = "notification"
// endregion

// region "home"
const val HOME_INFORMATION_BASE = "home"

const val HOME_LIST = "homes_list"

// region HW Units
const val HOME_HW_UNITS_BASE = "hw_units"

const val HW_ERROR_INFORMATION_BASE = "hw_error"
const val HW_RESTART_INFORMATION_BASE = "hw_restart"
// endregion

// region Rooms
const val HOME_ROOMS = "rooms"
const val HOME_ROOMS_ORDER = "roomsOrder"
// endregion

// region Tasks
const val HOME_TASKS_ORDER = "tasksOrder"
// endregion

// region HomeUnits
const val HOME_UNITS_BASE = "home_units"

enum class HomeUnitType(private val firebaseTableName: String) {
    UNKNOWN(""),
    HOME_ACTUATORS("actuators"),
    HOME_BLINDS("blinds"),

    // boolean sensor
    HOME_REED_SWITCHES("reed_switches"),
    HOME_MOTIONS("motions"),

    // combined actuator/sensor
    HOME_LIGHT_SWITCHES("light_switches"),
    // combined actuator/sensor
    HOME_LIGHT_SWITCHES_V2("light_switches_v2"),
    // float sensors
    HOME_TEMPERATURES("temperatures"),
    HOME_PRESSURES("pressures"),
    HOME_HUMIDITY("humidity"),
    HOME_GAS("gas"),
    HOME_GAS_PERCENT("gas_percent"),
    HOME_IAQ("iaq"),
    HOME_STATIC_IAQ("static_iaq"),
    HOME_CO2("co2"),
    HOME_BREATH_VOC("breathVoc");

    override fun toString(): String {
        return firebaseTableName
    }
}

fun String.toHomeUnitType(): HomeUnitType {
    return HomeUnitType.values().first { it.toString() == this }
}

const val HOME_VAL = "value"
const val HOME_VAL_LAST_UPDATE = "lastUpdateTime"
const val HOME_MIN_VAL = "min"
const val HOME_MIN_VAL_LAST_UPDATE = "minLastUpdateTime"
const val HOME_MAX_VAL = "max"
const val HOME_MAX_VAL_LAST_UPDATE = "maxLastUpdateTime"
const val HOME_LAST_TRIGGER_SOURCE = "lastTriggerSource"

const val LAST_TRIGGER_SOURCE_DEVICE_CONTROL = "device_control"
const val LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST = "room_home_units_list"
const val LAST_TRIGGER_SOURCE_TASK_LIST = "task_list"
const val LAST_TRIGGER_SOURCE_HOME_UNIT_DETAILS = "home_unit_details"
const val LAST_TRIGGER_SOURCE_BOOLEAN_APPLY = "boolean_apply"
const val LAST_TRIGGER_SOURCE_HW_UNIT = "hw_unit"
const val LAST_TRIGGER_SOURCE_HOME_UNIT_ADDED = "home_unit_added"

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
const val LOG_HW_UNIT_ERRORS = "error"
const val LOG_HW_UNIT = "hw_units"
const val LOG_THINGS_LOGS = "things_logs"
// endregion

// region "home"
const val RESTART_APP = "restart_app"
const val RESTART_PI = "restart_pi"
const val RESTART_BOARD = "restart_board"
// endregion
// endregion