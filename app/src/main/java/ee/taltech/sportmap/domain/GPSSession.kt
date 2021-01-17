package ee.taltech.sportmap.domain

class GPSSession {
    var id: Int = 0
    var name: String = ""
    var description: String = ""
    var recordedAt: String? = null
    var minSpeed: Int = 0
    var maxSpeed: Int = 0
    var apiSessionId: String? = null
    var locations = arrayListOf<Location>()

    constructor(
        name: String,
        description: String,
        recordedAt: String,
        minSpeed: Int,
        maxSpeed: Int): this(0, name, description, recordedAt, minSpeed, maxSpeed, null)

    constructor(id: Int, name: String, description: String, recordedAt: String, minSpeed: Int, maxSpeed: Int, apiSessionId: String?) {
        this.id = id
        this.name = name
        this.description = description
        this.recordedAt = recordedAt
        this.minSpeed = minSpeed
        this.maxSpeed = maxSpeed
        this.apiSessionId = apiSessionId
    }

    override fun toString(): String {
        return "ID: $id \n" +
                "Name: $name \n" +
                "Description: $description \n" +
                "RecordedAt: $recordedAt"
    }
}