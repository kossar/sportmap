package ee.taltech.sportmap.domain

class Location {
    var id: Int = 0
    var recorderAt: String = ""
    var latitude: Float = 0f
    var longitude: Float = 0f
    var accuracy: Float = 0f
    var altitude: Float = 0f
    var isSynced: Int = 0
    var verticalAccuracy: Float? = null
    var gpsLocationTypeId: String = ""

    constructor(
        recordedAt: String,
        latitude: Float,
        longitude: Float,
        accuracy: Float,
        altitude: Float,
        verticalAccuracy: Float,
        gpsLocationTypeId: String): this(0, recordedAt, latitude, longitude,accuracy, altitude, verticalAccuracy, gpsLocationTypeId, 0)

    constructor(id: Int, recordedAt: String, latitude: Float, longitude: Float,accuracy: Float, altitude: Float, verticalAccuracy: Float, gpsLocationTypeId: String, isSynced: Int) {
        this.id = id
        this.recorderAt = recordedAt
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.altitude = altitude
        this.verticalAccuracy = verticalAccuracy
        this.gpsLocationTypeId = gpsLocationTypeId
        this.isSynced = isSynced
    }

    override fun toString(): String {
        return "LOCATION: " +
                "ID: $id" +
                "RecordedAt: $recorderAt" +
                "LAT: $latitude" +
                "LON: $longitude" +
                "LOC_TYPE_ID: $gpsLocationTypeId"
    }
}