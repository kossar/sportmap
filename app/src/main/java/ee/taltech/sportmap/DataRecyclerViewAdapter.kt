package ee.taltech.sportmap

import android.app.AlertDialog
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.domain.User
import ee.taltech.sportmap.helpers.CheckNetwork
import ee.taltech.sportmap.helpers.LSHelper
import ee.taltech.sportmap.repository.GPSLocationRepository
import ee.taltech.sportmap.repository.UserRepository
import kotlinx.android.synthetic.main.map_actions.*
import kotlinx.android.synthetic.main.session_view.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DataRecyclerViewAdapter(
    private val context: Context,
    private val repo: GPSLocationRepository,
    private val userId: Int
): RecyclerView.Adapter<DataRecyclerViewAdapter.ViewHolder>(){
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }
    lateinit var dataSet: List<GPSSession>
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    var mResultCallback: IResult? = null
    var mVolleyService: VolleyService? = null

    fun refreshData() {
        Log.d(TAG, "refreshData")
        dataSet = repo.getUserGpsSessions(userId)
    }

    init {
        refreshData()
    }

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val sessionView = inflater.inflate(R.layout.session_view, parent, false)
        return ViewHolder(sessionView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gpsSession = dataSet[position]

        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(gpsSession.recordedAt!!)
        holder.itemView.textViewSessionName.text = dateFormat.format(date!!)
        holder.itemView.textViewOrderNr.text = (position + 1).toString()

        var currentLocation: Location? = null
        var distanceOverAll: Float = 0f
        var distanceTime: Long = 0L

        var isSynced = true
        if (gpsSession.apiSessionId == null) {
            isSynced = false
        }

        Log.d(TAG, "Location count: " + gpsSession.locations.count().toString())
        gpsSession.locations.forEach {
            val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(it.recorderAt)
            //Log.d(TAG, it.toString())

            if (it.isSynced == 0) {
                Log.d(TAG, "it ${it.isSynced} ${it.id}")
                isSynced = false
            }

            if (currentLocation == null) {
                currentLocation = Location("")
                currentLocation!!.latitude = it.latitude.toDouble()
                currentLocation!!.longitude = it.longitude.toDouble()
                currentLocation!!.time = dateTime!!.time
            } else {
                val location = Location("")
                location.latitude = it.latitude.toDouble()
                location.longitude = it.longitude.toDouble()
                location.time = dateTime!!.time
                if (it.gpsLocationTypeId == C.REST_LOCATIONID_LOC) {
                    distanceOverAll += location.distanceTo(currentLocation)
                    distanceTime += (location.time - currentLocation!!.time)
                    currentLocation = location
                }

            }
        }
        holder.itemView.textViewHistoryDistance.text = "%.2f".format(distanceOverAll) + "m"
        holder.itemView.textViewHistoryTime.text = LSHelper.getTimeString(distanceTime) + "h"
        holder.itemView.textViewHistoryPace.text = LSHelper.getPace(distanceTime, distanceOverAll) + "min/km"

        Log.d(TAG, "IsSynced $isSynced")
        if (isSynced) {
            holder.itemView.buttonSync.setText(R.string.synced_session)
            holder.itemView.buttonSync.isEnabled = false
            holder.itemView.buttonSync.isClickable = false
        }
        holder.itemView.buttonDeleteSession.setOnClickListener {
            Log.d(TAG, "Delete id: " + gpsSession.id.toString())
            showAlertDialogQuitConfirmation(gpsSession)
        }

        holder.itemView.buttonSaveGpx.setOnClickListener {
            Log.d(TAG, "SaveGpxOnClick")
            val userRepo = UserRepository(context).open()
            val user = userRepo.getUserById(userId)
            userRepo.close()
            Log.d(TAG, "USer: $user")
            val name = gpsSession.name + ".gpx"
            val gpx = createGpx(gpsSession.locations, user!!, gpsSession.description)

            val docDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

            val filePath = "$docDirectory/$name"
            Log.d(TAG, "Path: $filePath")
            val file = File(filePath)

            try {
                FileWriter(file).use { fileWriter -> fileWriter.append(gpx)
                Toast.makeText(context, "Succesfully saved Your GPX file!", Toast.LENGTH_SHORT).show()}
            } catch (e: IOException) {
                Toast.makeText(context, "Error saving a GPX! Please try again", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Exception in writing a file")
                Log.d(TAG, e.toString())
            }

        }

        holder.itemView.buttonSync.setOnClickListener{
            if (!CheckNetwork.isNetworkAvailable(context)) {
                Toast.makeText(context, "No Internet connection", Toast.LENGTH_SHORT).show()
            } else {
                initVolleyCallback(gpsSession.id)
                mVolleyService = VolleyService(mResultCallback, context, gpsSession,
                    delete = false,
                    saveBulk = true
                )
                mVolleyService!!.startRest()


                notifyDataSetChanged()
                refreshData()
            }

        }

        holder.itemView.buttonViewMapOfOldSession.setOnClickListener {
            val intent = Intent(context, MapsActivity::class.java)
            intent.putExtra(C.GPS_SESSION_ID_FROM_HISTORY, gpsSession.id)
            startActivity(context, intent, null)
        }
    }
    private fun  initVolleyCallback(gpsSessionId: Int) {
        class MyResult : IResult {

            override fun jwtSuccess(response: String, gpsSession: GPSSession) {
                Log.d(TAG, "Jwt:$response")
            }
            override fun gpsSessionSuccess(trackingId: String, gpsSession: GPSSession) {
                Log.d(TAG, "trackingID $trackingId")
                if (gpsSession.apiSessionId == null) {
                    repo.updateGpsSessionApiId(gpsSessionId, trackingId)
                }
            }
            override fun gpsSessError(error: VolleyError?) {
                Log.d(TAG, "Volley requester $error")
            }
            override fun successLocationInApi(location: Location, locType: String, locationId: Int) {
                Log.d(TAG, "successLocationInApi")
            }

            override fun errorLocationInApi(location: Location, locType: String) {
                Log.d(TAG, "Error with uploading location")
            }

            override fun bulkSaveSuccess(gpsSession: GPSSession) {
                var locationsNotInApiIds = arrayListOf<Int>()
                gpsSession.locations.forEach {
                    if (it.isSynced == 0) {
                        locationsNotInApiIds.add(it.id)
                    }
                }
                repo.bulkUpdateLocationsInApi(locationsNotInApiIds)
            }

        }
        mResultCallback = MyResult()
    }


    private fun showAlertDialogQuitConfirmation(gpsSession: GPSSession) {
        AlertDialog.Builder(context)
            .setTitle(R.string.alert_confirm_session_delete_title)
            .setMessage(R.string.alert_confirm_session_delete_description)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                R.string.yes
            ) { _, _ ->
                Toast.makeText(
                    context,
                    R.string.delete_activity,
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "GPS Session id from alertdialog ${gpsSession.id}")

                if (gpsSession.apiSessionId != null) {
                    mVolleyService = VolleyService(mResultCallback, context, gpsSession, delete = true)
                    mVolleyService!!.startRest()
                }
                repo.deleteGpsSession(gpsSession.id)
                refreshData()
                notifyDataSetChanged()
            }
            .setNegativeButton(R.string.no, null).show()
    }

    override fun getItemCount(): Int {
        return  dataSet.count()
    }



    private fun createGpx(
        locations: List<ee.taltech.sportmap.domain.Location>,
        user: User,
        description: String
    ): String {
        var header = "<gpx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns=\"http://www.topografix.com/GPX/1/0\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\" " +
                "version=\"1.0\" " +
                "creator=\"taltech.sportmap.ee\">\n" +
                "<metadata>\n" +
                "<author>\n" +
                "<name>${user.firstName} ${user.lastName}</name>\n" +
                "<email>${user.eMail}</email>\n" +
                "</author>\n" +
                "</metadata>\n "
        val footer = "</trkseg>\n" +
                "</trk>\n" +
                "</gpx>"
        var trk = "<trk>\n" +
                "<cmt>${description}</cmt>\n" +
                "<trkseg>\n"
        var waypoints = ""
        var trackPooints = ""
        locations.forEach {
            if (it.gpsLocationTypeId == C.REST_LOCATIONID_LOC) {
                trackPooints += "<trkpt lat=\"${it.latitude}\" lon=\"${it.longitude}\">\n" +
                        "<time>${it.recorderAt.split(".")[0]}</time>\n" +
                        "</trkpt>\n"
            }else if (it.gpsLocationTypeId == C.REST_LOCATIONID_CP) {
               waypoints += "<wpt lat=\"${it.latitude}\" lon=\"${it.longitude}\">\n" +
                        "<time>${it.recorderAt.split(".")[0]}</time>\n" +
                        "</wpt>\n"
            }

        }

        return header + waypoints + trk + trackPooints + footer
    }

    open inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}

