package ee.taltech.sportmap

class C {
    companion object {
        private const val PREFIX = "ee.taltech.sportmap."

        const val NOTIFICATION_CHANNEL = "default_channel"
        const val NOTIFICATION_ACTION_WP = PREFIX + "wp"
        const val NOTIFICATION_ACTION_CP = PREFIX + "cp"

        const val LOCATION_UPDATE_ACTION = PREFIX + "location_update"
        const val LOCATION_UPDATE_STOP = PREFIX + "location_stop"

        const val LOCATION_UPDATE_UI_ACTION_START = PREFIX + "location_update_ui_start"
        const val LOCATION_UPDATE_UI_ACTION_STOP = PREFIX + "location_update_ui_stop"

        const val LOCATION_UPDATE_UI_ACTION_START_LATITUDE = PREFIX + "location_update_ui_start.latitude"
        const val LOCATION_UPDATE_UI_ACTION_START_LONGITUDE = PREFIX + "location_update_ui_start.longitude"

        const val LOCATION_UPDATE_UI_ACTION_STOP_LATITUDE = PREFIX + "location_update_ui_stop.latitude"
        const val LOCATION_UPDATE_UI_ACTION_STOP_LONGITUDE = PREFIX + "location_update_ui_stop.longitude"

        const val LOCATION_UPDATE_ACTION_LATITUDE = PREFIX + "location_update.latitude"
        const val LOCATION_UPDATE_ACTION_LONGITUDE = PREFIX + "location_update.longitude"

        const val LOCATION_UPDATE_UI_CP_ACTION = PREFIX + "location_update_cp_ui"
        const val LOCATION_UPDATE_UI_WP_ACTION = PREFIX + "location_update_wp_ui"

        const val LOCATION_UPDATE_ACTION_CP_LATITUDE = PREFIX + "location_update_cp.latitude"
        const val LOCATION_UPDATE_ACTION_CP_LONGITUDE = PREFIX + "location_update_cp.longitude"

        const val LOCATION_UPDATE_ACTION_WP_LATITUDE = PREFIX + "location_update_wp.latitude"
        const val LOCATION_UPDATE_ACTION_WP_LONGITUDE = PREFIX + "location_update_wp.longitude"

        const val LOCATION_UPDATE_ACTION_OVERALL_DIRECT = PREFIX + "location_update.overall_direct"
        const val LOCATION_UPDATE_ACTION_OVERALL_TOTAL = PREFIX + "location_update.overall_total"
        const val LOCATION_UPDATE_ACTION_OVERALL_TIME = PREFIX + "location_update.overall_time"

        const val LOCATION_UPDATE_ACTION_CP_DIRECT = PREFIX + "location_update.cp_direct"
        const val LOCATION_UPDATE_ACTION_CP_TOTAL = PREFIX + "location_update.cp_total"
        const val LOCATION_UPDATE_ACTION_CP_TIME = PREFIX + "location_update.cp_time"

        const val LOCATION_UPDATE_ACTION_WP_DIRECT = PREFIX + "location_update.wp_direct"
        const val LOCATION_UPDATE_ACTION_WP_TOTAL = PREFIX + "location_update.wp_total"
        const val LOCATION_UPDATE_ACTION_WP_TIME = PREFIX + "location_update.wp_time"

        const val LOCATION_UPDATE_ACTION_LOCATION_BEARING = PREFIX + "location_bearing"

        const val NOTIFICATION_ID = 4321
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34;

        const val MAP_ACTIVITY_STOPPED_ACTION = PREFIX + "map_activity_stopped"
        const val MAP_ACTIVITY_RESTARTED_ACTION = PREFIX + "map_activity_restarted"

        const val START_MARKER = "start_marker"
        const val STOP_MARKER = "stop_marker"
        const val WP_MARKER = "wp_marker"
        const val CP_MARKER = "cp_marker"

        const val REST_BASE_URL = "https://sportmap.akaver.com/api/v1.0/"
        const val REST_REGISTER_ACCOUNT = REST_BASE_URL + "account/register"
        const val REST_LOGIN = REST_BASE_URL + "account/login"
        const val REST_USERNAME = "testMap@ttu.ee"
        const val REST_PASSWORD = "Sport.Map0"

        const val LOCAL_USER_ID = PREFIX + "local.user_id"
        const val LOCAL_GPS_SESSION_ID = PREFIX + "local.gps_session_id"

        const val REST_JWT_ACTION = REST_BASE_URL + "rest_jwt"
        const val REST_LOCATIONID_LOC = "00000000-0000-0000-0000-000000000001"
        const val REST_LOCATIONID_WP = "00000000-0000-0000-0000-000000000002"
        const val REST_LOCATIONID_CP = "00000000-0000-0000-0000-000000000003"

        const val WITHOUT_LOCATIONS = PREFIX + "without_locations"
        const val All_LOCATIONS = PREFIX + "all_locations"
        const val LOCATION_WP_AND_LOCATION_CP = PREFIX + "with_cp_and_wp"

        const val GPS_SESSION_ID_FROM_HISTORY = PREFIX + "gps_session_id_from_history"


    }

}