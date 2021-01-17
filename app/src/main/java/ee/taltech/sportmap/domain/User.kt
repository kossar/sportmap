package ee.taltech.sportmap.domain

import android.provider.ContactsContract

class User {
    var id: Int = 0
    var isActiveProfile = 0
    var firstName: String = ""
    var lastName: String = ""
    var eMail: String = ""
    var password: String = ""
    var gpsSessions = arrayListOf<GPSSession>()


    constructor(firstName: String, lastName: String, eMail: String, password: String): this(0, firstName, lastName, eMail, password, 0)

    constructor(id: Int, firstName: String, lastName: String, eMail: String, password: String, isActiveProfile: Int) {
        this.id = id
        this.firstName = firstName
        this.lastName = lastName
        this.eMail = eMail
        this.password = password
        this.isActiveProfile = isActiveProfile
    }

    override fun toString(): String {
        return "ID: " + id + "\n" +
                "Firstname: " + firstName + "\n" +
                "Lastname: " + lastName + "\n" +
                "E-Mail: " + eMail + "\n" +
                "PW: " + password
    }
}