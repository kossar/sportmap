package ee.taltech.sportmap.helpers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        // DB
        const val DATABASE_NAME = "map.db"
        const val DATABASE_VERSION = 2

        // Tables
        const val USERS_TABLE_NAME = "USERS"
        const val GPS_SESSION_TABLE_NAME = "GPS_SESSIONS"
        const val LOCATION_TABLE_NAME = "LOCATIONS"
        const val LOCATION_TYPES_TABLE_NAME = "LOCATION_TYPES"

        // User Table columns
        const val USER_ID = "_id"
        const val USER_FIRSTNAME = "firstname"
        const val USER_LASTNAME = "lastname"
        const val USER_E_MAIL = "email"
        const val USER_PASSWORD = "password"
        const val USER_IS_ACTIVE_PROFILE = "is_active_profile"

        //GPSSession Table columns
        const val GPS_SESSION_ID = "_id"
        const val GPS_SESSION_NAME = "name"
        const val GPS_SESSION_DESCRIPTION = "description"
        const val GPS_SESSION_RECORDED_AT = "recorded_at"
        const val GPS_SESSION_MIN_SPEED = "min_speed"
        const val GPS_SESSION_MAX_SPEED = "max_speed"
        const val GPS_SESSION_USER_FK_ID = "user_id"
        const val GPS_SESSION_API_SESSION_ID = "api_session_id"

        //Location Table columns
        const val LOCATION_ID = "_id"
        const val LOCATION_RECORDED_AT = "recorded_at"
        const val LOCATION_LATITUDE = "latitude"
        const val LOCATION_LONGITUDE = "longitude"
        const val LOCATION_ACCURACY = "accuracy"
        const val LOCATION_ALTITUDE = "altitude"
        const val LOCATION_VERTICAL_ACCURACY = "vertical_accuracy"
        const val LOCATION_IS_SYNCED = "is_synced"
        const val LOCATION_GPS_SESSION_FK_ID = "gps_session_id"
        const val LOCATION_LOCATION_TYPE_ID = "location_type_id"

        //Location Types table columns
        const val LOCATION_TYPE_ID = "_id"
        const val LOCATION_TYPE_NAME = "name"
        const val LOCATION_TYPE_DESCRIPTION = "description"

        //Create User table
        const val SQL_USER_CREATE_TABLE =
            "create table $USERS_TABLE_NAME (" +
                    "$USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$USER_FIRSTNAME TEXT NOT NULL, " +
                    "$USER_LASTNAME TEXT NOT NULL, " +
                    "$USER_E_MAIL TEXT NOT NULL, " +
                    "$USER_PASSWORD TEXT NOT NULL, " +
                    "$USER_IS_ACTIVE_PROFILE INTEGER NOT NULL);"

        // Create GPSSession table
        const val SQL_GPS_SESSION_CREATE_TABLE =
            "create table $GPS_SESSION_TABLE_NAME (" +
                    "$GPS_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$GPS_SESSION_NAME TEXT NOT NULL, " +
                    "$GPS_SESSION_DESCRIPTION TEXT NOT NULL, " +
                    "$GPS_SESSION_RECORDED_AT TEXT NOT NULL, " +
                    "$GPS_SESSION_MIN_SPEED INTEGER NOT NULL, " +
                    "$GPS_SESSION_MAX_SPEED INTEGER NOT NULL, " +
                    "$GPS_SESSION_USER_FK_ID INTEGER NOT NULL, " +
                    "$GPS_SESSION_API_SESSION_ID TEXT, " +
                    "FOREIGN KEY ($GPS_SESSION_USER_FK_ID) REFERENCES $USERS_TABLE_NAME ($USER_ID)" +
                    "ON DELETE CASCADE" +
                    ");"

        // Create Location table
        const val SQL_LOCATION_CREATE_TABLE =
            "create table $LOCATION_TABLE_NAME (" +
                    "$LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$LOCATION_RECORDED_AT TEXT NOT NULL, " +
                    "$LOCATION_LATITUDE REAL NOT NULL, " +
                    "$LOCATION_LONGITUDE REAL NOT NULL, " +
                    "$LOCATION_ACCURACY REAL NOT NULL, " +
                    "$LOCATION_ALTITUDE REAL NOT NULL, " +
                    "$LOCATION_VERTICAL_ACCURACY REAL, " +
                    "$LOCATION_IS_SYNCED INTEGER NOT NULL, " +
                    "$LOCATION_LOCATION_TYPE_ID TEXT NOT NULL, " +
                    "$LOCATION_GPS_SESSION_FK_ID INTEGER NOT NULL, " +
                    "FOREIGN KEY ($LOCATION_GPS_SESSION_FK_ID) REFERENCES $GPS_SESSION_TABLE_NAME ($GPS_SESSION_ID)" +
                    "ON DELETE CASCADE" +
                    ");"

        // Create Location Type table
        const val SQL_LOCATION_TYPE_CREATE_TABLE =
            "create table $LOCATION_TYPES_TABLE_NAME (" +
                    "$LOCATION_TYPE_ID TEXT PRIMARY KEY, " +
                    "$LOCATION_TYPE_NAME TEXT NOT NULL, " +
                    "$LOCATION_TYPE_DESCRIPTION TEXT);"


        // Delete tables
        const val SQL_DELETE_TABLE_USER = "DROP TABLE IF EXISTS $USERS_TABLE_NAME;"
        const val SQL_DELETE_TABLE_GPS_SESSION = "DROP TABLE IF EXISTS $GPS_SESSION_TABLE_NAME;"
        const val SQL_DELETE_TABLE_LOCATION = "DROP TABLE IF EXISTS $LOCATION_TABLE_NAME;"
        const val SQL_DELETE_TABLE_LOCATION_TYPE = "DROP TABLE IF EXISTS $LOCATION_TYPES_TABLE_NAME;"

        // Insert 3 location types
        const val SQL_INSERT_LOCATION_TYPE_LOC = "INSERT INTO $LOCATION_TYPES_TABLE_NAME (" +
                "$LOCATION_TYPE_ID, $LOCATION_TYPE_NAME, $LOCATION_TYPE_DESCRIPTION) " +
                "VALUES ('00000000-0000-0000-0000-000000000001', 'LOC', 'Regular periodic location update');"

        const val SQL_INSERT_LOCATION_TYPE_WP = "INSERT INTO $LOCATION_TYPES_TABLE_NAME (" +
                "$LOCATION_TYPE_ID, $LOCATION_TYPE_NAME, $LOCATION_TYPE_DESCRIPTION) " +
                "VALUES ('00000000-0000-0000-0000-000000000002', 'WP', 'Waypoint - temporary location, used as navigation aid');"

        const val SQL_INSERT_LOCATION_TYPE_CP = "INSERT INTO $LOCATION_TYPES_TABLE_NAME (" +
                "$LOCATION_TYPE_ID, $LOCATION_TYPE_NAME, $LOCATION_TYPE_DESCRIPTION) " +
                "VALUES ('00000000-0000-0000-0000-000000000003', 'CP', 'Checkpoint - found on terrain the location marked on the paper map');"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_USER_CREATE_TABLE)
        db?.execSQL(SQL_GPS_SESSION_CREATE_TABLE)
        db?.execSQL(SQL_LOCATION_CREATE_TABLE)
        db?.execSQL(SQL_LOCATION_TYPE_CREATE_TABLE)
        db?.execSQL(SQL_INSERT_LOCATION_TYPE_LOC)
        db?.execSQL(SQL_INSERT_LOCATION_TYPE_WP)
        db?.execSQL(SQL_INSERT_LOCATION_TYPE_CP)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_TABLE_USER)
        db?.execSQL(SQL_DELETE_TABLE_GPS_SESSION)
        db?.execSQL(SQL_DELETE_TABLE_LOCATION)
        db?.execSQL(SQL_DELETE_TABLE_LOCATION_TYPE)
        onCreate(db)
//        if (newVersion > oldVersion) {
//            db?.execSQL("ALTER TABLE $LOCATION_TABLE_NAME ADD COLUMN $LOCATION_IS_SYNCED INTEGER DEFAULT 0");
//        }
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        if (!db?.isReadOnly()!!) {
            db.setForeignKeyConstraintsEnabled (true)
        }
    }


}