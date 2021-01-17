package ee.taltech.sportmap

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.helpers.LSHelper
import ee.taltech.sportmap.repository.GPSLocationRepository
import kotlinx.android.synthetic.main.map_actions.*
import kotlinx.android.synthetic.main.navigation_layout.*
import kotlinx.android.synthetic.main.options_layout.*
import java.text.SimpleDateFormat
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var mMap: GoogleMap
    private var polyLine: Polyline? = null

    //private var marker: Marker? = null
    private var markerWP: Marker? = null

    private var latestLatLng: LatLng? = null
    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private var locationServiceActive = false
    private var userWantsToQuit = false

    private var isDirectionUp = true
    private var locationBearing = 0.0f
    private var isCentered = true

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    private var sessionSetup: AlertDialog.Builder? = null
    var userId: Int? = null
    private lateinit var gpsLocationRepo: GPSLocationRepository
    private var gpsSessionId: Int = 0
    private var gpsSession: GPSSession? = null

    private var isHistoryActivity: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigation_layout)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        gpsLocationRepo = GPSLocationRepository(this).open()

        if (!checkPermissions()) {
            requestPermissions()
        }
        if (intent != null && intent.hasExtra(C.LOCAL_USER_ID)) {
            userId = intent.extras!!.getInt(C.LOCAL_USER_ID)
            Log.d(TAG, "UserId: " + userId.toString())
        }

        if (intent != null && intent.hasExtra(C.GPS_SESSION_ID_FROM_HISTORY)) {
            isHistoryActivity = true
            //gpsSession!!.id = intent.extras!!.getInt(C.GPS_SESSION_ID_FROM_HISTORY)
            gpsSessionId = intent.extras!!.getInt(C.GPS_SESSION_ID_FROM_HISTORY)
        }

        if (!isHistoryActivity) {
            mapsActivity()
        }

    }

    private fun mapsActivity() {
        Log.d(TAG, "mapsActivity")
        // safe to call every time
        createNotificationChannel()

        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_UI_CP_ACTION)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_UI_WP_ACTION)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_UI_ACTION_START)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_UI_ACTION_STOP)

        //Start-Stop Button onClick
        imageButtonStartStop.setOnClickListener {
            imageButtonStartStopOnClick(it)
        }

        //Checkpoint Button onClick
        imageButtonCP.setOnClickListener {
            Log.d(TAG, "imageButtonCP")
            if (locationServiceActive) {
                sendBroadcast(Intent(C.NOTIFICATION_ACTION_CP))
            }
        }

        //Way point Button onClick
        imageButtonWP.setOnClickListener {
            Log.d(TAG, "imageButtonWP")
            if (locationServiceActive) {
                sendBroadcast(Intent(C.NOTIFICATION_ACTION_WP))
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady")
        mMap = googleMap

        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))

        var defaultOrLatest = LatLng(59.3972281, 24.6558104)
        if (latestLatLng != null) {
            defaultOrLatest = latestLatLng as LatLng
        }
        if (!isHistoryActivity) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }

            mMap.isMyLocationEnabled = true
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultOrLatest))


//        mMap.uiSettings.isZoomControlsEnabled = true

//        if (isHistoryActivity) {
//            //historyMap(gpsSession!!.id)
//            historyMap(gpsSessionId)
//        }
        Log.d(TAG, gpsSessionId.toString())
        if (gpsSessionId != 0) {
            historyMap(gpsSessionId)
        }
        if (locationServiceActive) {
            if (polyLine != null) {
                polyLine!!.remove()

            }
            polyLine = mMap.addPolyline(LSHelper.getMapPolylineOptions())


        }

        Log.d(TAG, "onMapReady end")

    }



    private fun updateMap(lat: Double, lon: Double) {
        Log.d(TAG, "updateMap")
        val center = LatLng(lat, lon)
        Log.d(TAG, "IsCEntered $isCentered, IsDirectionUp $isDirectionUp, Bearing $locationBearing")

        latestLatLng = center

        if (polyLine != null) {
            polyLine!!.remove()
        }

        polyLine = mMap.addPolyline(LSHelper.getMapPolylineOptions())

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(center))
        val cameraPosition = CameraPosition.Builder()
            .target(center).zoom(17f).bearing(locationBearing).build()

        if (isCentered) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()


        if (!isHistoryActivity) {
            val intent = Intent(C.MAP_ACTIVITY_RESTARTED_ACTION)
            sendBroadcast(intent)

            Log.d(TAG, intent.toString())
            LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver,
                broadcastReceiverIntentFilter
            )

        }
        if (locationServiceActive) {
            if (polyLine != null) {
                polyLine!!.remove()
                polyLine = mMap.addPolyline(LSHelper.getMapPolylineOptions())
            }

        }

    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
        if (!isHistoryActivity) {
            val intent = Intent(C.MAP_ACTIVITY_STOPPED_ACTION)
            //LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            sendBroadcast(intent)
            Log.d(TAG, intent.toString())
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        }

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        gpsLocationRepo.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putBoolean("userWantsToQuit", userWantsToQuit)
        outState.putBoolean("locationServiceActive", locationServiceActive)
        outState.putBoolean("isDirectionUp", isDirectionUp)
        outState.putBoolean("isCentered", isCentered)
        outState.putFloat("bearing", locationBearing)

        if (userId != null) {
            outState.putInt("userId", userId!!)
        }
        outState.putInt("gpsSessionId", gpsSessionId)

        outState.putBoolean("isHistoryActivity", isHistoryActivity)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d(TAG, "onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        userWantsToQuit = savedInstanceState.getBoolean("userWantsToQuit")
        locationServiceActive = savedInstanceState.getBoolean("locationServiceActive")
        isHistoryActivity = savedInstanceState.getBoolean("isHistoryActivity")
        gpsSessionId = savedInstanceState.getInt("gpsSessionId")
        isDirectionUp = savedInstanceState.getBoolean("isDirectionUp")
        isCentered = savedInstanceState.getBoolean("isCentered")
        locationBearing = savedInstanceState.getFloat("bearing")

        if (locationServiceActive) {
            imageButtonStartStop.setImageResource(R.drawable.stop)
//            gpsSession = gpsLocationRepo.getGpsSession(gpsSessionId, C.LOCATION_WP_AND_LOCATION_CP)
//            Log.d(TAG, gpsSession.toString())
//            gpsSession!!.locations.forEach {
//                if (it.gpsLocationTypeId == C.REST_LOCATIONID_WP) {
//                    addMarker(it.latitude.toDouble(), it.longitude.toDouble(), C.CP_MARKER)
//                } else if (it.gpsLocationTypeId == C.REST_LOCATIONID_CP) {
//                    addMarker(it.latitude.toDouble(), it.longitude.toDouble(), C.CP_MARKER)
//                }
//            }
        } else {
            imageButtonStartStop.setImageResource(R.drawable.ic_play_arrow)
        }
        if (isDirectionUp) {
            buttonDirectionUp.setImageResource(R.drawable.dir_free)
        }else {
            buttonDirectionUp.setImageResource(R.drawable.dir_up)
        }
        if (isCentered) {
            buttonCenter.setImageResource(R.drawable.free)
        }else {
            buttonCenter.setImageResource(R.drawable.center)
        }
    }

    // ============================================== NOTIFICATION CHANNEL CREATION =============================================
    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel")
        // when on 8 Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                C.NOTIFICATION_CHANNEL,
                "Default channel",
                NotificationManager.IMPORTANCE_LOW
            );

            //.setShowBadge(false).setSound(null, null);

            channel.description = "Default channel"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    // ============================================== CLICK HANDLERS =============================================
    private fun imageButtonStartStopOnClick(view: View) {
        Log.d(TAG, "buttonStartStopOnClick. locationServiceActive: $locationServiceActive")
        // try to start/stop the background service
        //val imageButtonStartStop = findViewById<ImageButton>(R.id.imageButtonStartStop)
        if (locationServiceActive) {
            val fromBackPress = false
            showAlertDialogQuitConfirmation(fromBackPress)

        } else {
            if (userId == null) {
                notLoggedInActionOnStartService()
            } else {
                sessionSetup = showSessionSetup()
                sessionSetup!!.show()
            }
        }
    }

    private fun showSessionSetup(): AlertDialog.Builder {
        Log.d(TAG, "showSessionSetup")
        val inflater = LayoutInflater.from(this)
        val activity = inflater.inflate(R.layout.sport_activity_name_description, null)

        val activityDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        activityDialog.setTitle(R.string.activity_dialog_title)
        activityDialog.setMessage(R.string.activity_dialog_description)
        activityDialog.setView(activity)
        val activityName = activity.findViewById<EditText>(R.id.editTextActivityName)
        val activityDescription = activity.findViewById<EditText>(R.id.editTextActivityDescription)

        activityDialog.setPositiveButton(
            R.string.start,
            DialogInterface.OnClickListener { dialog, whichButton ->
                gpsSession = GPSSession(
                    Date().toString(),
                    Date().toString(),
                    dateFormat.format(Date()),
                    6 * 60,
                    18 * 60
                )
                if (activityName.text.trim().isNotEmpty()) {
                    gpsSession!!.name = activityName.text.trim().toString()
                }
                if (activityDescription.text.trim().isNotEmpty()) {
                    gpsSession!!.description = activityDescription.text.trim().toString()
                }

                gpsSession!!.id = gpsLocationRepo.saveGPSSession(gpsSession!!, userId!!)
                gpsSessionId = gpsSession!!.id
                Log.d(TAG, "GPS Session id: " + gpsSession!!.id.toString())
                startLocationService()
                dialog.dismiss()
            })

        activityDialog.setNegativeButton(R.string.cancel
        ) { dialog, _ -> // TODO Auto-generated method stub
            dialog.dismiss()
        }
        activityDialog.create()

        return activityDialog
    }

    private fun startLocationService() {
        Log.d(TAG, "startLocationService()")
        // clear the track on map
        LSHelper.clearMapPolylineOptions()
        mMap.clear()
        userWantsToQuit = false
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra(C.LOCAL_USER_ID, userId)
        intent.putExtra(C.LOCAL_GPS_SESSION_ID, gpsSession!!.id)
        if (Build.VERSION.SDK_INT >= 26) {
            // starting the FOREGROUND service
            // service has to display non-dismissable notification within 5 secs
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        imageButtonStartStop.setImageResource(R.drawable.stop)
        //buttonStartStop.text = "STOP"
        locationServiceActive = !locationServiceActive
    }

    private fun notLoggedInActionOnStartService() {
        AlertDialog.Builder(this)
            .setTitle(R.string.not_logged_in)
            .setMessage(R.string.log_in_dialog_msg)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                R.string.ok
            ) { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }.show()
    }


    private fun addMarker(lat: Double, lon: Double, markerType: String) {
        Log.d(TAG, "addMArker")
        val pos = LatLng(lat, lon)
        var markerColor: Float = BitmapDescriptorFactory.HUE_BLUE
        when (markerType) {
            C.START_MARKER -> {
                markerColor = BitmapDescriptorFactory.HUE_GREEN
            }
            C.STOP_MARKER -> {
                markerColor = BitmapDescriptorFactory.HUE_RED
            }
            C.CP_MARKER -> {
                //Remove WP when adding new Cp
                if (markerWP != null) {
                    markerWP!!.remove()
                }
                markerColor = BitmapDescriptorFactory.HUE_BLUE
            }
            C.WP_MARKER -> {
                //Remove previous WP before adding new WP
                if (markerWP != null) {
                    markerWP!!.remove()
                }
                markerColor = BitmapDescriptorFactory.HUE_VIOLET
            }
        }
        if (markerType == C.WP_MARKER) {
            //Add new WP marker and assign it for a variable for modifications
            markerWP = mMap.addMarker((getMarkerOptions(pos, markerType, markerColor)))
        } else {
            //Add other marker types
            mMap.addMarker((getMarkerOptions(pos, markerType, markerColor)))
        }

    }

    private fun getMarkerOptions(
        latLng: LatLng,
        markerType: String,
        markerColor: Float
    ): MarkerOptions {
        Log.d(TAG, "getMarkerOptions")
        return MarkerOptions()
            .position(latLng)
            .title(markerType)
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

    }

    // ============================================== PERMISSION HANDLING =============================================
    // Returns the current state of the permissions needed.
    private fun checkPermissions(): Boolean {
        Log.d(TAG, "checkPermissions")
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        Log.d(TAG, "requestPermissions")
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(
                TAG,
                "Displaying permission rationale to provide additional context."
            )
            Snackbar.make(
                findViewById(R.id.navigation_layout),
                "Hey, i really need to access GPS!",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("OK", View.OnClickListener {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        C.REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                })
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                C.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode === C.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.count() <= 0) { // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
                Toast.makeText(this, "User interaction was cancelled.", Toast.LENGTH_SHORT).show()
            } else if (grantResults[0] === PackageManager.PERMISSION_GRANTED) {// Permission was granted.
                Log.i(TAG, "Permission was granted")
                Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show()
            } else { // Permission denied.
                Snackbar.make(
                    findViewById(R.id.navigation_layout),
                    "You denied GPS! What can I do?",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Settings", View.OnClickListener {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
                    .show()
            }
        }
    }

    // ============================================== SESSION HISTORY HANDLING =============================================

    private fun historyMap(gpsSessionId: Int) {
        Log.d(TAG, "historyMao")
        gpsSession = gpsLocationRepo.getGpsSession(gpsSessionId, C.All_LOCATIONS)
        Log.d(TAG, "historyMap GPS Session: " + gpsSession.toString())
        if (!locationServiceActive) {
            LSHelper.clearMapPolylineOptions()
        }
        var firstLocation: Location? = null

        var currentLocation: Location? = null
        var distanceOverAll: Float = 0f
        var distanceTime: Long = 0L

        var locationCP: Location? = null
        var distanceCPTotal = 0f
        var distanceCPTime = 0L

        var locationWP: Location? = null
        var distanceWPTotal = 0f
        var distanceWPTime = 0L

        if (gpsSession!!.locations.size == 0) {
            return
        }
        gpsSession!!.locations.forEach {
            Log.d(TAG, it.toString())

            val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(it.recorderAt)

            if (currentLocation == null) {
                currentLocation = Location("")
                currentLocation!!.latitude = it.latitude.toDouble()
                currentLocation!!.longitude = it.longitude.toDouble()
                currentLocation!!.time = dateTime!!.time

                firstLocation = Location("")
                firstLocation!!.latitude = it.latitude.toDouble()
                firstLocation!!.longitude = it.longitude.toDouble()
                firstLocation!!.time = dateTime!!.time

                locationCP = currentLocation
                locationWP = currentLocation
            } else {
                val newLocation = Location("")
                newLocation.latitude = it.latitude.toDouble()
                newLocation.longitude = it.longitude.toDouble()
                newLocation.time = dateTime!!.time

                if (it.gpsLocationTypeId == C.REST_LOCATIONID_LOC) {
                    distanceOverAll += newLocation.distanceTo(currentLocation)
                    distanceTime += (newLocation.time - currentLocation!!.time)

                    distanceCPTotal += newLocation.distanceTo(currentLocation)
                    distanceCPTime += (newLocation.time - currentLocation!!.time)

                    distanceWPTotal += newLocation.distanceTo(currentLocation)
                    distanceWPTime += (newLocation.time - currentLocation!!.time)
                    currentLocation = newLocation
                }

            }


            Log.d(TAG, it.gpsLocationTypeId)
            if (it.gpsLocationTypeId == C.REST_LOCATIONID_CP) {
                Log.d(TAG, "addMarkerCP")
                locationCP = currentLocation
                locationWP = currentLocation
                distanceCPTotal = 0f
                distanceCPTime = 0L

                distanceWPTotal = 0f
                distanceWPTime = 0L
                addMarker(currentLocation!!.latitude, currentLocation!!.longitude, C.CP_MARKER)
            } else if (it.gpsLocationTypeId == C.REST_LOCATIONID_WP) {
                locationWP = currentLocation
                distanceWPTotal = 0f
                distanceWPTime = 0L
                addMarker(currentLocation!!.latitude, currentLocation!!.longitude, C.WP_MARKER)
            }

            if (!locationServiceActive) {
                LSHelper.addToMapPolylineOptions(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
            }
        }



        addMarker(firstLocation!!.latitude, firstLocation!!.longitude, C.START_MARKER)

        // If location service is active, then we dont need to redraw polyline, and we dont need stop marker
        if (!locationServiceActive) {
            addMarker(currentLocation!!.latitude, currentLocation!!.longitude, C.STOP_MARKER)
//            if (polyLine != null) {
//                polyLine!!.remove()
//            }
//            mMap.addPolyline(LSHelper.getMapPolylineOptions())
        }

        if (polyLine != null) {
            polyLine!!.remove()
        }
        polyLine = mMap.addPolyline(LSHelper.getMapPolylineOptions())

//        val centerLat = (currentLocation!!.latitude + firstLocation!!.latitude) / 2
//        val centerLon = (currentLocation!!.longitude + firstLocation!!.longitude) / 2
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(centerLat, centerLon)))
//        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
//        mMap.moveCamera(
//            (CameraUpdateFactory.newLatLng(
//                LatLng(
//                    currentLocation!!.latitude,
//                    currentLocation!!.longitude
//                )
//            ))
//        )

       val  cameraPosition = CameraPosition.Builder()
            .target(LatLng(
                currentLocation!!.latitude,
                currentLocation!!.longitude
            )).zoom(17f).bearing(locationBearing).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))



        textViewOverallDirect.text = getString(R.string.meter, "%.2f".format(currentLocation!!.distanceTo(firstLocation)))
        textViewOverallTotal.text = getString(R.string.meter, "%.2f".format(distanceOverAll))
        textViewOverallDuration.text = getString(R.string.hour, LSHelper.getTimeString(distanceTime))
        textViewOverallSpeed.text = getString(R.string.min_km, LSHelper.getPace(distanceTime, distanceOverAll))

        textViewCPDirect.text = getString(R.string.meter, "%.2f".format(currentLocation!!.distanceTo(locationCP)))
        textViewCPTotal.text = getString(R.string.meter, "%.2f".format(distanceCPTotal))
        textViewCPDuration.text = getString(R.string.hour, LSHelper.getTimeString(distanceCPTime) )
        textViewCPSpeed.text = getString(R.string.min_km, LSHelper.getPace(distanceCPTime, distanceCPTotal))

        textViewWPDirect.text = getString(R.string.meter, "%.2f".format(currentLocation!!.distanceTo(locationWP)))
        textViewWPTotal.text = getString(R.string.meter, "%.2f".format(distanceWPTotal))
        textViewWPDuration.text = getString(R.string.hour, LSHelper.getTimeString(distanceWPTime))
        textViewWPSpeed.text = getString(R.string.min_km, LSHelper.getPace(distanceWPTime, distanceWPTotal))



    }

    // ============================================== QUIT ACTIVITY HANDLING =============================================

    @Override
    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        if (!locationServiceActive) {
            super.onBackPressed()
        } else {
            val fromBackPress = true
            showAlertDialogQuitConfirmation(fromBackPress)
        }

    }

    private fun showAlertDialogQuitConfirmation(fromBackPress: Boolean) {
        Log.d(TAG, "showAlertDialogQuitConfirmation")
        AlertDialog.Builder(this)
            .setTitle(R.string.question_quit_tracking)
            .setMessage(R.string.instruction_for_quit_tracking)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                R.string.yes
            ) { _, _ ->
                Toast.makeText(
                    this,
                    R.string.stopping,
                    Toast.LENGTH_SHORT
                ).show()

                // stopping the service
                stopService(Intent(this, LocationService::class.java))
                imageButtonStartStop.setImageResource(R.drawable.ic_play_arrow)
                locationServiceActive = !locationServiceActive
                if (fromBackPress) {
                    super.onBackPressed()
                }
            }
            .setNegativeButton(R.string.no, null).show()
    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action.toString())
            when (intent!!.action) {
                C.LOCATION_UPDATE_ACTION -> {
                    textViewOverallDirect.text = getString(R.string.meter, intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALL_DIRECT, 0.0f).toInt()
                        .toString())

                    textViewOverallTotal.text = getString(R.string.meter, intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, 0.0f).toInt()
                        .toString())


                    var duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME, 0)
                    textViewOverallDuration.text = getString(R.string.hour, LSHelper.getTimeString(duration))
                    textViewOverallSpeed.text = getString(R.string.min_km, LSHelper.getPace(
                        duration,
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, 0.0f)
                    ))

                    textViewCPDirect.text = getString(R.string.meter, intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CP_DIRECT, 0.0f).toInt()
                        .toString())

                    textViewCPTotal.text = getString(R.string.meter,  intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, 0.0f).toInt()
                        .toString())


                    duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_CP_TIME, 0)
                    textViewCPDuration.text = getString(R.string.hour, LSHelper.getTimeString(duration))
                    textViewCPSpeed.text = getString(R.string.min_km, LSHelper.getPace(
                        duration,
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, 0.0f)
                    ))

                    textViewWPDirect.text = getString(R.string.meter, intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WP_DIRECT, 0.0f).toInt()
                        .toString())

                    textViewWPTotal.text = getString(R.string.meter, intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, 0.0f).toInt()
                        .toString())

                    duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_WP_TIME, 0)
                    textViewWPDuration.text = getString(R.string.hour, LSHelper.getTimeString(duration))
                    textViewWPSpeed.text = getString(R.string.min_km, LSHelper.getPace(
                        duration,
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, 0.0f)
                    ))

                    locationBearing = if (isDirectionUp) {
                        0.0f
                    } else {
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_LOCATION_BEARING, 0.0f)
                    }

                    updateMap(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0)
                    )
                }
                C.LOCATION_UPDATE_STOP -> {
                }
                C.LOCATION_UPDATE_UI_CP_ACTION -> {
                    Log.d(TAG, "CP in broadcast receiver")
                    addMarker(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_CP_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_CP_LONGITUDE, 0.0),
                        C.CP_MARKER
                    )
                }
                C.LOCATION_UPDATE_UI_WP_ACTION -> {
                    addMarker(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_WP_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_WP_LONGITUDE, 0.0),
                        C.WP_MARKER
                    )
                }
                C.LOCATION_UPDATE_UI_ACTION_START -> {
                    addMarker(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_UI_ACTION_START_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_UI_ACTION_START_LONGITUDE, 0.0),
                        C.START_MARKER
                    )
                }
                C.LOCATION_UPDATE_UI_ACTION_STOP -> {
                    addMarker(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_UI_ACTION_STOP_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_UI_ACTION_STOP_LONGITUDE, 0.0),
                        C.STOP_MARKER
                    )
                }
            }
        }

    }

    fun buttonDirectionOnClick(view: View) {
        Log.d(TAG, "buttonDirectionOnClick start")
        isCentered = true
        Log.d(TAG, "buttonDirectionOnClick iscentered = truet")
        isDirectionUp = !isDirectionUp
        Log.d(TAG, "buttonDirectionOnClick !dirup")
        if (isDirectionUp) {
            buttonDirectionUp.setImageResource(R.drawable.dir_free)
            locationBearing = 0.0f
        }else {
            buttonDirectionUp.setImageResource(R.drawable.dir_up)
        }
        Log.d(TAG, "buttonDirectionOnClick end")

    }

    fun buttonCenterOnClick(view: View) {
        isCentered = !isCentered
        if (isCentered) {
            buttonCenter.setImageResource(R.drawable.free)
        }else {
            buttonCenter.setImageResource(R.drawable.center)
        }
    }

    fun buttonCompassOnClick(view: View) {
        val intent = Intent(this, CompassActivity::class.java)
        startActivity(intent)
    }
}