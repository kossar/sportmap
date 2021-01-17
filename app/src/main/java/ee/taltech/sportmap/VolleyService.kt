package ee.taltech.sportmap

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.repository.UserRepository
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class VolleyService internal constructor(
    resultCallback: IResult?,
    context: Context,
    gpsSession: GPSSession,
    delete: Boolean = false,
    saveBulk: Boolean = false
) {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }
    var mResultCallback: IResult? = null
    var mContext: Context
    var mGpsSession: GPSSession? = null
    var jwt: String? = null
    var trackingSessionId: String? = null
    var isSaveBulkLocations: Boolean = false
    var deleteSession: Boolean = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    fun startRest() {
        Log.d(TAG, "getRestToken")
        var userRepo = UserRepository(mContext).open()
        var user = userRepo.getActiveUser()
        userRepo.close()

        var handler = WebApiSingletonHandler.getInstance(mContext)
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("email", user!!.eMail)
        requestJsonParameters.put("password", user!!.password)

        Log.d(TAG, user!!.eMail)

        var httpRequest = JsonObjectRequest(
            Request.Method.POST,
            C.REST_LOGIN,
            requestJsonParameters,
            { response ->
                Log.d(TAG, "Response: " + response.toString())
                jwt = response.getString("token")

                if (mGpsSession!!.apiSessionId == null && !deleteSession) {
                    startRestTrackingSession()
                } else if (deleteSession) {
                    deleteSession()
                } else {
                    trackingSessionId = mGpsSession!!.apiSessionId
                    if (isSaveBulkLocations) {
                        bulkSaveLocations()
                    }
                }
                mResultCallback?.jwtSuccess(
                    jwt!!, mGpsSession!!
                )

            },
            { error ->
                Log.d(TAG, error.toString())
                mResultCallback?.gpsSessError(error);
            }
        )

        handler.addToRequestQueue(httpRequest)

    }
    private fun startRestTrackingSession() {
        var handler = WebApiSingletonHandler.getInstance(mContext)
        val requestJsonParameters = JSONObject()
        if (mGpsSession != null) {
            requestJsonParameters.put("name", mGpsSession!!.name)
            requestJsonParameters.put("description", mGpsSession!!.description)
            requestJsonParameters.put("paceMin", mGpsSession!!.minSpeed)
            requestJsonParameters.put("paceMax", mGpsSession!!.maxSpeed)
        } else {
            requestJsonParameters.put("name", Date().toString())
            requestJsonParameters.put("description", Date())
            requestJsonParameters.put("paceMin", 6 * 60)
            requestJsonParameters.put("paceMax", 18 * 60)
        }

        var httpRequest = object : JsonObjectRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsSessions",
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                trackingSessionId = response.getString("id")
                Log.d(TAG, "trackingid $trackingSessionId")

                if (isSaveBulkLocations) {
                    bulkSaveLocations()
                }
                mResultCallback?.gpsSessionSuccess(
                    trackingSessionId!!, mGpsSession!!
                )
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }


        handler.addToRequestQueue(httpRequest)
    }

    fun saveRestLocation(location: Location, locationType: String, locId: Int = 0) {
        Log.d(TAG, "Volley service saveRestLocation")
        if (jwt == null || trackingSessionId == null) {
            return
        }


        var handler = WebApiSingletonHandler.getInstance(mContext)
        val requestJsonParameters = JSONObject()

        Log.d(TAG, "Loc time: ${dateFormat.format(Date(location.time))}")
        requestJsonParameters.put("recordedAt", dateFormat.format(Date(location.time)))

        requestJsonParameters.put("latitude", location.latitude)
        requestJsonParameters.put("longitude", location.longitude)
        requestJsonParameters.put("accuracy", location.accuracy)
        requestJsonParameters.put("altitude", location.altitude)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestJsonParameters.put("verticalAccuracy", location.verticalAccuracyMeters)
        }
        requestJsonParameters.put("gpsSessionId", trackingSessionId)
        requestJsonParameters.put("gpsLocationTypeId", locationType)


        var httpRequest = object : JsonObjectRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsLocations",
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, "Succcess saving location")
                Log.d(TAG, response.toString())
                mResultCallback?.successLocationInApi(
                    location, locationType, locId
                )
            },
            Response.ErrorListener { error ->
                mResultCallback?.errorLocationInApi(
                    location, locationType
                )
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun deleteSession() {
        Log.d(TAG, "Volley service deleteSession")
        if (jwt == null) {
            return
        }

        var handler = WebApiSingletonHandler.getInstance(mContext)
        val requestJsonParameters = JSONObject()

        var httpRequest = object : JsonObjectRequest(
            Request.Method.DELETE,
            C.REST_BASE_URL + "/GpsSessions/" + mGpsSession!!.apiSessionId,
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, "Delete success")
            },
            Response.ErrorListener { error ->
                Log.d(TAG, "Delete error")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun bulkSaveLocations() {
        Log.d(TAG, "Volley service bulksave")
        if (jwt == null || trackingSessionId == null) {
            return
        }

        val jsonArray = gpsLocationsToJson(mGpsSession!!)

        val handler = WebApiSingletonHandler.getInstance(mContext)

        var httpRequest = object : StringRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsLocations/bulkupload/" + trackingSessionId,
            // jsonArray,

            Response.Listener { response ->
                // val resp = response.getJSONObject(0)
                Log.d(TAG, "Volleyservice bulksave success")
                // Log.d(TAG, resp.toString())
                Log.d(TAG, "$response")
                mResultCallback?.bulkSaveSuccess(
                    mGpsSession!!
                )
            },
            Response.ErrorListener { error ->
                Log.d(TAG, "Error in bulk save")
                Log.d(TAG, error.toString())
            }
        )
        {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8;"
            }
            override fun getBody(): ByteArray {
                Log.d(TAG, "getBody()")
                Log.d(TAG, "${jsonArray.toString(1)}")
                return jsonArray.toString().encodeToByteArray()
            }
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                Log.d(TAG, "Headers $headers")
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }


    fun gpsLocationsToJson(gpsSession: GPSSession): JSONArray {
        var jsonArray = JSONArray()
        gpsSession.locations.forEach {
            var jsonObject = JSONObject()
            if (it.isSynced == 0){
                val location = convertLocation(it)
                jsonObject.put("recordedAt", location.time)

                jsonObject.put("latitude", location.latitude)
                jsonObject.put("longitude", location.longitude)
                jsonObject.put("accuracy", location.accuracy)
                jsonObject.put("altitude", location.altitude)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    jsonObject.put("verticalAccuracy", location.verticalAccuracyMeters)
                }
                jsonObject.put("gpsSessionId", gpsSession.apiSessionId)
                jsonObject.put("gpsLocationTypeId", it.gpsLocationTypeId)
                jsonArray.put(jsonObject)
            }
        }
        Log.d(TAG, jsonArray.toString())
        return jsonArray
    }

    private fun convertLocation(loc: ee.taltech.sportmap.domain.Location): Location {
        val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(loc.recorderAt)
        val location = Location("")
        location.time = dateTime!!.time
        location.longitude = loc.longitude.toDouble()
        location.latitude = loc.latitude.toDouble()
        location.accuracy = loc.accuracy
        location.altitude = loc.altitude.toDouble()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            location.verticalAccuracyMeters = loc.verticalAccuracy!!
        }

        return location
    }


    init {
        mResultCallback = resultCallback
        mContext = context
        mGpsSession = gpsSession
        isSaveBulkLocations = saveBulk
        deleteSession = delete
    }
}