package ee.taltech.sportmap.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import ee.taltech.sportmap.C
import ee.taltech.sportmap.helpers.DbHelper
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.domain.Location
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GPSLocationRepository(val context: Context) {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }
    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    fun open(): GPSLocationRepository {
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase

        return this
    }

    fun close() {
        dbHelper.close()
    }

    // Location data

    fun saveGPSSession(gpsSession: GPSSession, userId: Int): Int {
        Log.d(TAG, "saveGPSSession")
        val contentValues = ContentValues()
        contentValues.put(DbHelper.GPS_SESSION_NAME, gpsSession.name)
        contentValues.put(DbHelper.GPS_SESSION_DESCRIPTION, gpsSession.description)
        contentValues.put(DbHelper.GPS_SESSION_RECORDED_AT, gpsSession.recordedAt)
        contentValues.put(DbHelper.GPS_SESSION_MAX_SPEED, gpsSession.maxSpeed)
        contentValues.put(DbHelper.GPS_SESSION_MIN_SPEED, gpsSession.minSpeed)
        contentValues.put(DbHelper.GPS_SESSION_USER_FK_ID, userId)
        if (gpsSession.apiSessionId != null) {
            contentValues.put(DbHelper.GPS_SESSION_API_SESSION_ID, gpsSession.apiSessionId)
        }

        return db.insert(DbHelper.GPS_SESSION_TABLE_NAME, null, contentValues).toInt()

    }

    fun getGpsSession(id: Int, withLocationTypes: String): GPSSession? {
        Log.d(TAG, "getGpsSession")
        Log.d(TAG, "gpsid: " + id.toString())
        var gpsSession: GPSSession? = null
        val columns = arrayOf(
            DbHelper.GPS_SESSION_ID,
            DbHelper.GPS_SESSION_NAME,
            DbHelper.GPS_SESSION_DESCRIPTION,
            DbHelper.GPS_SESSION_RECORDED_AT,
            DbHelper.GPS_SESSION_MIN_SPEED,
            DbHelper.GPS_SESSION_MAX_SPEED,
            DbHelper.GPS_SESSION_API_SESSION_ID

        )
        val where = DbHelper.GPS_SESSION_ID + " = ? "
        val args = arrayOf(id.toString())
        val cursor = db.query(DbHelper.GPS_SESSION_TABLE_NAME, columns, where, args, null, null, null)
        while (cursor.moveToNext()) {
                gpsSession = GPSSession(
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_NAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_RECORDED_AT)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_MIN_SPEED)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_MAX_SPEED)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_API_SESSION_ID))
                )
        }
        cursor.close()
        if (gpsSession != null && withLocationTypes != C.WITHOUT_LOCATIONS) {
            if (withLocationTypes == C.All_LOCATIONS) {
                gpsSession.locations = getGpsSessionLocations(gpsSession.id, C.All_LOCATIONS)

            }else if (withLocationTypes == C.LOCATION_WP_AND_LOCATION_CP) {
                gpsSession.locations = getGpsSessionLocations(gpsSession.id, C.LOCATION_WP_AND_LOCATION_CP)

            }
        }

        Log.d(TAG, "GPS session from db" + gpsSession.toString())
        return gpsSession
    }

    fun getUserGpsSessions(userId: Int): ArrayList<GPSSession> {
        Log.d(TAG, "getUserGpsSessions Start")
        var gpsSessions = ArrayList<GPSSession>()
        val columns = arrayOf(
            DbHelper.GPS_SESSION_ID,
            DbHelper.GPS_SESSION_NAME,
            DbHelper.GPS_SESSION_DESCRIPTION,
            DbHelper.GPS_SESSION_RECORDED_AT,
            DbHelper.GPS_SESSION_MIN_SPEED,
            DbHelper.GPS_SESSION_MAX_SPEED,
            DbHelper.GPS_SESSION_API_SESSION_ID
        )
        val where = DbHelper.GPS_SESSION_USER_FK_ID + " = ?"
        val args = arrayOf(userId.toString())
        val cursor = db.query(DbHelper.GPS_SESSION_TABLE_NAME, columns, where, args, null, null, DbHelper.GPS_SESSION_ID + " DESC")
        while (cursor.moveToNext()) {
            gpsSessions.add(
                GPSSession(
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_NAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_RECORDED_AT)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_MIN_SPEED)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.GPS_SESSION_MAX_SPEED)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.GPS_SESSION_API_SESSION_ID))
                )
            )
        }
        cursor.close()
        gpsSessions.forEach {
            it.locations = getGpsSessionLocations(it.id, C.All_LOCATIONS)
        }
        Log.d(TAG, "getUserGpsSessions End")

        return gpsSessions

    }

    fun updateGpsSessionApiId(gpsSessionId: Int, gpSessionApiId: String) {
        Log.d(TAG, "updateGpsSessionApiId $gpsSessionId $gpSessionApiId")
        val contentValues = ContentValues()
        contentValues.put(DbHelper.GPS_SESSION_API_SESSION_ID, gpSessionApiId)
        db.update(DbHelper.GPS_SESSION_TABLE_NAME, contentValues, DbHelper.GPS_SESSION_ID + " = ?", arrayOf(gpsSessionId.toString()))
    }

    fun getGpsSessionLocations(gpsSessionId: Int, whichTypes: String): ArrayList<Location> {
        Log.d(TAG, "getGpsSessionLocations")
        var locations = ArrayList<Location>()
        val columns = arrayOf(
            DbHelper.LOCATION_ID,
            DbHelper.LOCATION_RECORDED_AT,
            DbHelper.LOCATION_LATITUDE,
            DbHelper.LOCATION_LONGITUDE,
            DbHelper.LOCATION_ACCURACY,
            DbHelper.LOCATION_ALTITUDE,
            DbHelper.LOCATION_VERTICAL_ACCURACY,
            DbHelper.LOCATION_LOCATION_TYPE_ID,
            DbHelper.LOCATION_IS_SYNCED
        )
        var where = DbHelper.LOCATION_GPS_SESSION_FK_ID + " = ? "
        var args = arrayOf(gpsSessionId.toString())

        if (whichTypes == C.LOCATION_WP_AND_LOCATION_CP) {
            where = DbHelper.LOCATION_GPS_SESSION_FK_ID + " = ? AND (" +
                    DbHelper.LOCATION_LOCATION_TYPE_ID + " = ? OR " +
                    DbHelper.LOCATION_LOCATION_TYPE_ID + " = ? )"
            args = arrayOf(gpsSessionId.toString(), C.REST_LOCATIONID_WP, C.REST_LOCATIONID_CP)
        }

        val cursor = db.query(DbHelper.LOCATION_TABLE_NAME, columns, where, args, null, null, null)
        while (cursor.moveToNext()) {
            locations.add(
                Location(
                    cursor.getInt(cursor.getColumnIndex(DbHelper.LOCATION_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_RECORDED_AT)),
                    cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_LATITUDE)),
                    cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_LONGITUDE)),
                    cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_ACCURACY)),
                    cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_ALTITUDE)),
                    cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_VERTICAL_ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_LOCATION_TYPE_ID)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.LOCATION_IS_SYNCED))
                )
            )
        }
        cursor.close()
        return locations
    }

    fun saveUserLocation(location: android.location.Location, locationTypeId: String, gpsSessionId: Int, isSynced: Int) {
        Log.d(TAG, "saveUserLocation")
        Log.d(TAG, "LocType: $locationTypeId, gosSessId: $gpsSessionId, isSynced: $isSynced")
        val contentValues = ContentValues()
        contentValues.put(DbHelper.LOCATION_RECORDED_AT, dateFormat.format(Date(location.time)))
        contentValues.put(DbHelper.LOCATION_LATITUDE, location.latitude)
        contentValues.put(DbHelper.LOCATION_LONGITUDE, location.longitude)
        contentValues.put(DbHelper.LOCATION_ACCURACY, location.accuracy)
        contentValues.put(DbHelper.LOCATION_ALTITUDE, location.altitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            contentValues.put(DbHelper.LOCATION_VERTICAL_ACCURACY, location.verticalAccuracyMeters)
        }
        contentValues.put(DbHelper.LOCATION_IS_SYNCED, isSynced)
        contentValues.put(DbHelper.LOCATION_LOCATION_TYPE_ID, locationTypeId)
        contentValues.put(DbHelper.LOCATION_GPS_SESSION_FK_ID, gpsSessionId)
        if (db.isOpen){
            db.insert(DbHelper.LOCATION_TABLE_NAME, null, contentValues)
        }else {
            db = dbHelper.writableDatabase
            db.insert(DbHelper.LOCATION_TABLE_NAME, null, contentValues)
            db.close()
        }


    }

    fun updateLocationInApi(locationId: Int, isSynced: Int) {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.LOCATION_IS_SYNCED, isSynced)
        db.update(DbHelper.LOCATION_TABLE_NAME, contentValues, DbHelper.GPS_SESSION_ID + " = ?", arrayOf(locationId.toString()))
    }

    fun bulkUpdateLocationsInApi(locationIds: List<Int>) {
        val locationStringIds = Array (locationIds.size) { i -> locationIds[i].toString()}

        var lengthOfLocations = "?"
        for (i in 1..locationIds.size){
            lengthOfLocations += ",?"
        }

        val args = locationIds.joinToString(",")
        Log.d(TAG, "bulkUpdateLocationsInApi Args: $args")
        val contentValues = ContentValues()
        contentValues.put(DbHelper.LOCATION_IS_SYNCED, 1)
        val count = db.update(DbHelper.LOCATION_TABLE_NAME, contentValues, DbHelper.LOCATION_ID + " IN ($lengthOfLocations)", locationStringIds)
        Log.d(TAG, "Count $count")
    }

    fun hasWayPoint(gpsSessionId: Int): Boolean {
        val columns = arrayOf(
            DbHelper.LOCATION_GPS_SESSION_FK_ID,
            DbHelper.LOCATION_LOCATION_TYPE_ID
        )
        val where = DbHelper.LOCATION_GPS_SESSION_FK_ID + " = ? AND " + DbHelper.LOCATION_LOCATION_TYPE_ID + " = ? "
        val args = arrayOf(gpsSessionId.toString(), C.REST_LOCATIONID_WP)
        val cursor = db.query(DbHelper.LOCATION_TABLE_NAME, columns, where, args, null, null, null)
        if(cursor.count <= 0){
            cursor.close()
            return false
        }
        return true
    }


    fun getMarkedPoint(gpsSessionId: Int, locationTypeId: String): Location? {
        var location: Location? = null
        val columns = arrayOf(
            DbHelper.LOCATION_ID,
            DbHelper.LOCATION_RECORDED_AT,
            DbHelper.LOCATION_LATITUDE,
            DbHelper.LOCATION_LONGITUDE,
            DbHelper.LOCATION_ACCURACY,
            DbHelper.LOCATION_ALTITUDE,
            DbHelper.LOCATION_VERTICAL_ACCURACY,
            DbHelper.LOCATION_TYPE_ID,
            DbHelper.LOCATION_IS_SYNCED
        )

        val where = DbHelper.LOCATION_GPS_SESSION_FK_ID + " = ? AND " + DbHelper.LOCATION_LOCATION_TYPE_ID + " = ? "
        val args = arrayOf(gpsSessionId.toString(), locationTypeId)
        val cursor = db.query(DbHelper.LOCATION_TABLE_NAME, columns, where, args, null, null, null)
        while (cursor.moveToNext()) {
            location = Location(
                cursor.getInt(cursor.getColumnIndex(DbHelper.LOCATION_ID)),
                cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_RECORDED_AT)),
                cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_LATITUDE)),
                cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_LONGITUDE)),
                cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_ACCURACY)),
                cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_ALTITUDE)),
                cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_VERTICAL_ACCURACY)),
                cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_TYPE_ID)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.LOCATION_IS_SYNCED))

            )
        }
        cursor.close()
        return location
    }

    fun replaceOldWayPoint(wayPoint: Location) {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.LOCATION_LOCATION_TYPE_ID, C.REST_LOCATIONID_LOC)
        db.update(DbHelper.LOCATION_TABLE_NAME, contentValues, DbHelper.LOCATION_ID + " = ?", arrayOf(wayPoint.id.toString()))
    }

    fun deleteGpsSession(sessionId: Int) {
        Log.d(TAG, "deleteGpsSession $sessionId")
        db.delete(DbHelper.GPS_SESSION_TABLE_NAME, DbHelper.GPS_SESSION_ID + " = ?", arrayOf(sessionId.toString()))
    }
}