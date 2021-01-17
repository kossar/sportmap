package ee.taltech.sportmap

import com.android.volley.VolleyError
import ee.taltech.sportmap.domain.GPSSession
import ee.taltech.sportmap.domain.Location

import org.json.JSONObject


interface IResult {
    fun jwtSuccess(response: String, gpsSession: GPSSession)
    fun gpsSessionSuccess(trackingId: String, gpsSession: GPSSession)
    fun gpsSessError(error: VolleyError?)
    fun successLocationInApi(location: android.location.Location, locType: String, locationId: Int = 0)
    fun errorLocationInApi(location: android.location.Location, locType: String)
    fun bulkSaveSuccess(gpsSession: GPSSession)
}