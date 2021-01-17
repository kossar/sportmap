package ee.taltech.sportmap.domain

class LocationType {

    var id: String = ""
    var name: String = ""
    var description: String = ""

    constructor(id: String, name: String, description: String) {
        this.id = id
        this.name = name
        this.description = description
    }
}