package ee.taltech.sportmap

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.*
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.helpers.CheckNetwork
import ee.taltech.sportmap.helpers.LSHelper
import ee.taltech.sportmap.repository.GPSLocationRepository
import ee.taltech.sportmap.repository.UserRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    // The desired intervals for location updates. Inexact. Updates may be more or less frequent.
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private lateinit var gpsLocationRepo: GPSLocationRepository

    private val mLocationRequest: LocationRequest = LocationRequest()
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback? = null

    // last received location
    private var currentLocation: Location? = null

    private var distanceOverallDirect = 0f
    private var distanceOverallTotal = 0f
    private var locationStart: Location? = null
    private var distanceOverallTime: Long = 0L

    private var distanceCPDirect = 0f
    private var distanceCPTotal = 0f
    private var locationCP: Location? = null
    private var distanceCPTime: Long = 0L

    private var distanceWPDirect = 0f
    private var distanceWPTotal = 0f
    private var distanceWPTime: Long = 0L

    private var locationWP: Location? = null

    private var mapActivityStopped = false
    private var cpCollectionWhenMapStopped = mutableListOf<Location>()
    private var newWPWhenMapStopped = false

    //var activeUserId: Int? = null
    var gpsSessionId: Int? = null
    private var jwt: String? = null
    private var trackingSessionId: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    var mResultCallback: IResult? = null
    var mVolleyService: VolleyService? = null

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        gpsLocationRepo = GPSLocationRepository(this).open()

        initVolleyCallback()


        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_CP)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_WP)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
        broadcastReceiverIntentFilter.addAction(C.MAP_ACTIVITY_STOPPED_ACTION)
        broadcastReceiverIntentFilter.addAction(C.MAP_ACTIVITY_RESTARTED_ACTION)


        registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }

        //getRestToken()



        getLastLocation()

        createLocationRequest()
        requestLocationUpdates()

    }

    private fun startTracking() {
        var gpsSession = gpsLocationRepo.getGpsSession(gpsSessionId!!, C.WITHOUT_LOCATIONS)
        mVolleyService = VolleyService(mResultCallback, this, gpsSession!!)
        mVolleyService!!.startRest()
    }

    private fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")

        try {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(
                TAG,
                "Lost location permission. Could not request updates. $unlikely"
            )
        }
    }

    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")
        if (location.accuracy > 100) {
            return
        }
        if (currentLocation == null){
            locationStart = location
            locationCP = location
            locationWP = location

            val intentStart = Intent(C.LOCATION_UPDATE_UI_ACTION_START)
            intentStart.putExtra(
                C.LOCATION_UPDATE_UI_ACTION_START_LATITUDE,
                locationStart!!.latitude
            )
            intentStart.putExtra(
                C.LOCATION_UPDATE_UI_ACTION_START_LONGITUDE,
                locationStart!!.longitude
            )
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentStart)
        } else {
            Log.d(TAG, "distance to last: " + location.distanceTo(currentLocation).toString())
            if (location.distanceTo(currentLocation) < 0.5) {
                return
            }
            distanceOverallDirect = location.distanceTo(locationStart)
            distanceOverallTotal += location.distanceTo(currentLocation)
            distanceOverallTime += (location.time - currentLocation!!.time)
            Log.d(TAG, "Time: " + location.time)

            distanceCPDirect = location.distanceTo(locationCP)
            distanceCPTotal += location.distanceTo(currentLocation)
            distanceCPTime += (location.time - currentLocation!!.time)

            distanceWPDirect = location.distanceTo(locationWP)
            distanceWPTotal += location.distanceTo(currentLocation)
            distanceWPTime += (location.time - currentLocation!!.time)
        }
        // save the location for calculations
        currentLocation = location
        //Add to polyline
        LSHelper.addToMapPolylineOptions(location.latitude, location.longitude)
        showNotification()

        //Log.d(TAG, "sessid: " + gpsSessionId.toString())
        if (CheckNetwork.isNetworkAvailable(this)) {
            mVolleyService!!.saveRestLocation(location, C.REST_LOCATIONID_LOC)
        }else{
            gpsLocationRepo.saveUserLocation(location, C.REST_LOCATIONID_LOC, gpsSessionId!!, 0)
        }

        // broadcast new location to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, location.latitude)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, location.longitude)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_DIRECT, distanceOverallDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, distanceOverallTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME, distanceOverallTime)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_DIRECT, distanceCPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, distanceCPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_TIME, distanceCPTime)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_DIRECT, distanceWPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, distanceWPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_TIME, distanceWPTime)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_LOCATION_BEARING, location.bearing)


        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }

    private fun createLocationRequest() {
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        mLocationRequest.setMaxWaitTime(UPDATE_INTERVAL_IN_MILLISECONDS)
    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient.lastLocation
                    .addOnCompleteListener { task -> if (task.isSuccessful) {
                        Log.w(TAG, "task successfull");
                        if (task.result != null){
                            onNewLocation(task.result!!)
                        }
                    } else {

                        Log.w(TAG, "Failed to get location." + task.exception)
                    }}
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        //stop location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)

        val intentStop = Intent(C.LOCATION_UPDATE_UI_ACTION_STOP)
        intentStop.putExtra(C.LOCATION_UPDATE_UI_ACTION_STOP_LATITUDE, currentLocation!!.latitude)
        intentStop.putExtra(C.LOCATION_UPDATE_UI_ACTION_STOP_LONGITUDE, currentLocation!!.longitude)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentStop)
        // remove notifications
        NotificationManagerCompat.from(this).cancelAll()


        unregisterReceiver(broadcastReceiver)


        // broadcast stop to UI
        val intent = Intent(C.LOCATION_UPDATE_STOP)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        //Close repo
        gpsLocationRepo.close()

    }
    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent != null) {
//            if (intent.hasExtra(C.LOCAL_USER_ID)) {
//                activeUserId = intent!!.extras!!.getInt(C.LOCAL_USER_ID)
//                Log.d(TAG, "ACTIVE user intent: " + activeUserId.toString())
//            }
            if (intent.hasExtra(C.LOCAL_GPS_SESSION_ID)){
                gpsSessionId = intent.extras!!.getInt(C.LOCAL_GPS_SESSION_ID)
                startTracking()
                Log.d(TAG, "GPS Sess id from intent: " + gpsSessionId.toString())
            }
        }



       // Log.d(TAG, "user_id: " + activeUserId)
        // set counters and locations to 0/null
        currentLocation = null
        locationStart = null
        locationCP = null
        locationWP = null

        distanceOverallDirect = 0f
        distanceOverallTotal = 0f
        distanceCPDirect = 0f
        distanceCPTotal = 0f
        distanceWPDirect = 0f
        distanceWPTotal = 0f


        showNotification()

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    fun showNotification(){
        val intentCp = Intent(C.NOTIFICATION_ACTION_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_WP)

        val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
        val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)

        val notifyview = RemoteViews(packageName, R.layout.map_actions)

        notifyview.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
        notifyview.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)


        notifyview.setTextViewText(
            R.id.textViewOverallDirect, getString(
                R.string.meter, "%.2f".format(
                    distanceOverallDirect
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewOverallTotal, getString(
                R.string.meter, "%.2f".format(
                    distanceOverallTotal
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewOverallDuration, getString(
                R.string.hour, LSHelper.getTimeString(
                    distanceOverallTime
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewOverallSpeed, getString(
                R.string.min_km, LSHelper.getPace(
                    distanceOverallTime,
                    distanceOverallTotal
                )
            )
        )

        notifyview.setTextViewText(
            R.id.textViewWPDirect, getString(
                R.string.meter, "%.2f".format(
                    distanceWPDirect
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewWPTotal, getString(
                R.string.meter, "%.2f".format(
                    distanceWPTotal
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewWPDuration, getString(
                R.string.hour, LSHelper.getTimeString(
                    distanceWPTime
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewWPSpeed, getString(
                R.string.min_km, LSHelper.getPace(
                    distanceWPTime,
                    distanceWPTotal
                )
            )
        )

        notifyview.setTextViewText(
            R.id.textViewCPDirect, getString(
                R.string.meter, "%.2f".format(
                    distanceCPDirect
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewCPTotal, getString(
                R.string.meter, "%.2f".format(
                    distanceCPTotal
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewCPDuration, getString(
                R.string.hour, LSHelper.getTimeString(
                    distanceCPTime
                )
            )
        )
        notifyview.setTextViewText(
            R.id.textViewCPSpeed, getString(
                R.string.min_km, LSHelper.getPace(
                    distanceCPTime,
                    distanceCPTotal
                )
            )
        )

        // construct and show notification
        var builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_gps_fixed)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setColor(resources.getColor(R.color.black))


        builder.setContent(notifyview)

        // Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())

    }

    fun addNewCPToUI(location: Location){
        Log.d(TAG, "Adding CP")
        if (mapActivityStopped) {
            cpCollectionWhenMapStopped.add(location)
            Log.d(TAG, "Len after 1 cp; " + cpCollectionWhenMapStopped.count())
        } else {
            val intent = Intent(C.LOCATION_UPDATE_UI_CP_ACTION)
            intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_LATITUDE, location.latitude)
            intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_LONGITUDE, location.longitude)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }
    fun addNewWPToUI(location: Location){
        Log.d(TAG, "Adding WP")
         if (!mapActivityStopped) {
             Log.d(TAG, "!mapActivityStopped || (newWPWhenMapStopped && !mapActivityStopped)")
            val intent = Intent(C.LOCATION_UPDATE_UI_WP_ACTION)
            intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_LATITUDE, location.latitude)
            intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_LONGITUDE, location.longitude)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } else if (mapActivityStopped) {
            // if mapactivity stopped then set boolean to track if latest WP arrived
            newWPWhenMapStopped = true
        }
    }
    fun upDateUIAfterUIRestart() {
        Log.d(TAG, "upDateUIAfterUIRestart")
        Log.d(
            TAG,
            "Length of cp list: before adding" + cpCollectionWhenMapStopped.count().toString()
        )
        cpCollectionWhenMapStopped.forEach{
            addNewCPToUI(it)
        }
        cpCollectionWhenMapStopped.clear()
        Log.d(
            TAG,
            "Length of cp list: after adding" + cpCollectionWhenMapStopped.count().toString()
        )

        if (newWPWhenMapStopped) {
            addNewWPToUI(locationWP!!)
        }

        //Reset new Wp when map stopped, because just added it and map is not stopped
        newWPWhenMapStopped = false
    }
    private inner class InnerBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action.toString())
            when(intent!!.action){
                C.NOTIFICATION_ACTION_WP -> {
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f

                    showNotification()
                    locationWP?.let { addNewWPToUI(it) }
                    // IsSynced is true (isSynced == 1), because this is not needed in API
                    gpsLocationRepo.saveUserLocation(locationWP!!, C.REST_LOCATIONID_WP, gpsSessionId!!, 1)
                }
                C.NOTIFICATION_ACTION_CP -> {
                    locationCP = currentLocation
                    distanceCPDirect = 0f
                    distanceCPTotal = 0f
                    distanceCPTime = 0

                    //reset WP also, since we know exactly where we are on the map
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    distanceWPTime = 0

                    showNotification()
                    locationCP?.let { addNewCPToUI(it) }
                    if (CheckNetwork.isNetworkAvailable(this@LocationService)) {
                        mVolleyService!!.saveRestLocation(locationCP!!, C.REST_LOCATIONID_CP)
                    }else{
                        gpsLocationRepo.saveUserLocation(locationCP!!, C.REST_LOCATIONID_CP, gpsSessionId!!, 0)
                    }
                }
                C.MAP_ACTIVITY_STOPPED_ACTION -> {
                    Log.d(TAG, "mapActivity stopped intent")
                    mapActivityStopped = true
                }
                C.MAP_ACTIVITY_RESTARTED_ACTION -> {
                    Log.d(TAG, "mapActivity restarted intent")
                    mapActivityStopped = false
                    upDateUIAfterUIRestart()

                }
            }
        }

    }

    private fun  initVolleyCallback() {
        class MyResult : IResult {

            override fun jwtSuccess(response: String, gpsSession: GPSSession) {
                Log.d(TAG, "Jwt:" + response)
            }
            override fun gpsSessionSuccess(trackingId: String, gpsSession: GPSSession) {
                Log.d(TAG, "trackingID $trackingId")
                gpsLocationRepo.updateGpsSessionApiId(gpsSessionId!!, trackingId)
            }
            override fun gpsSessError(error: VolleyError?) {
                Log.d(TAG, "Volley requester " + error)
            }

            override fun successLocationInApi(location: Location, locType: String, locationId: Int) {
                Log.d(TAG, "successLocationInApi")

                gpsLocationRepo.saveUserLocation(location, locType, gpsSessionId!!, 1)
            }

            override fun errorLocationInApi(location: Location, locType: String) {
                gpsLocationRepo.saveUserLocation(location, locType, gpsSessionId!!, 0)
            }

            override fun bulkSaveSuccess(gpsSession: GPSSession) {
                TODO("Not yet implemented")
            }

        }
        mResultCallback = MyResult()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        TODO("Not yet implemented")
    }
    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)

    }
}