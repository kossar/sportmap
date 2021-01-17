package ee.taltech.sportmap.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import ee.taltech.sportmap.helpers.DbHelper
import ee.taltech.sportmap.domain.User

class UserRepository(val context: Context) {

    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    private val allColumns = arrayOf(
        DbHelper.USER_ID,
        DbHelper.USER_FIRSTNAME,
        DbHelper.USER_LASTNAME,
        DbHelper.USER_E_MAIL,
        DbHelper.USER_PASSWORD,
        DbHelper.USER_IS_ACTIVE_PROFILE
    )

    fun open(): UserRepository {
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase

        return this
    }

    fun close() {
        dbHelper.close()
    }

    fun saveUser(user: User): Int {
        // Check if user e-mail is not already in db
        if (userEMailInDb(user.eMail)) {
            return 0
        }

        val contentValues = ContentValues()
        contentValues.put(DbHelper.USER_FIRSTNAME, user.firstName)
        contentValues.put(DbHelper.USER_LASTNAME, user.lastName)
        contentValues.put(DbHelper.USER_E_MAIL, user.eMail)
        contentValues.put(DbHelper.USER_PASSWORD, user.password)
        contentValues.put(DbHelper.USER_IS_ACTIVE_PROFILE, user.isActiveProfile)
        return db.insert(DbHelper.USERS_TABLE_NAME, null, contentValues).toInt()

    }
    private fun userEMailInDb(eMail: String): Boolean {
        val columns = arrayOf(DbHelper.USER_E_MAIL)
        val cursor = db.query(DbHelper.USERS_TABLE_NAME, columns, DbHelper.USER_E_MAIL +" = ?", arrayOf(eMail), null, null, null)
        if(cursor.count <= 0){
            cursor.close()
            return false
        }
        return true
    }

    //User log in
    fun userLogIn(eMail: String, password: String): User? {
        var user: User? = null
        //val columns =  arrayOf(DbHelper.USER_E_MAIL, DbHelper.USER_PASSWORD)
        val where = DbHelper.USER_E_MAIL + " = ? AND " + DbHelper.USER_PASSWORD + " = ? "
        val arguments = arrayOf(eMail, password)

        val cursor = db.query(DbHelper.USERS_TABLE_NAME, allColumns, where, arguments, null, null, null)

        while (cursor.moveToNext()) {
            user = User(
                cursor.getInt(0),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_FIRSTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_LASTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_E_MAIL)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_PASSWORD)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.USER_IS_ACTIVE_PROFILE))
            )
        }
        cursor.close()

        return user

    }

    fun hasActiveUser(): Boolean {

        val column = arrayOf(DbHelper.USER_IS_ACTIVE_PROFILE)
        val cursor = db.query(DbHelper.USERS_TABLE_NAME, column, DbHelper.USER_IS_ACTIVE_PROFILE + " = ?", arrayOf(1.toString()), null, null, null)
        if(cursor.count <= 0){
            cursor.close()
            return false
        }
        return true
    }
    fun updateUser(user: User){
        val contentValues = ContentValues()
        contentValues.put(DbHelper.USER_FIRSTNAME, user.firstName)
        contentValues.put(DbHelper.USER_LASTNAME, user.lastName)
        contentValues.put(DbHelper.USER_E_MAIL, user.eMail)
        contentValues.put(DbHelper.USER_PASSWORD, user.password)
        contentValues.put(DbHelper.USER_IS_ACTIVE_PROFILE, user.isActiveProfile)
        db.update(DbHelper.USERS_TABLE_NAME, contentValues, DbHelper.USER_ID + " = ?", arrayOf(user.id.toString()))
    }

    fun getUserById(id: Int): User? {
        var user: User? = null

        val cursor = db.query(DbHelper.USERS_TABLE_NAME, allColumns, DbHelper.USER_ID + " = ?", arrayOf(id.toString()), null, null, null)

        while (cursor.moveToNext()) {
            user = User(
                cursor.getInt(0),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_FIRSTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_LASTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_E_MAIL)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_PASSWORD)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.USER_IS_ACTIVE_PROFILE))
            )
        }
        cursor.close()

        return user
    }

    fun deActivateUser() {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.USER_IS_ACTIVE_PROFILE, 0.toString())
        db.update(DbHelper.USERS_TABLE_NAME, contentValues, DbHelper.USER_IS_ACTIVE_PROFILE + " = ?", arrayOf(1.toString()))
    }
    //Active User for default log in
    fun getActiveUser(): User? {
        var user: User? = null

        val cursor = db.query(DbHelper.USERS_TABLE_NAME, allColumns, DbHelper.USER_IS_ACTIVE_PROFILE + " = ?", arrayOf(1.toString()), null, null, null)

        while (cursor.moveToNext()) {
            user = User(
                cursor.getInt(0),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_FIRSTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_LASTNAME)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_E_MAIL)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_PASSWORD)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.USER_IS_ACTIVE_PROFILE))
            )
        }
        cursor.close()

        return user

    }

    fun deleteUser(user: User) {
        db.delete(DbHelper.USERS_TABLE_NAME, DbHelper.USER_ID + "= ?", arrayOf(user.id.toString()))
    }
    
    fun getAllUsers(): List<User> {
        val users = ArrayList<User>()

        val columns =
            arrayOf(
                DbHelper.USER_ID,
                DbHelper.USER_FIRSTNAME,
                DbHelper.USER_LASTNAME,
                DbHelper.USER_E_MAIL,
                DbHelper.USER_PASSWORD,
                DbHelper.USER_IS_ACTIVE_PROFILE
            )


        val cursor = db.query(DbHelper.USERS_TABLE_NAME, columns, null, null, null, null, null)

        while (cursor.moveToNext()) {
            users.add(
                User(
                    cursor.getInt(0),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_FIRSTNAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_LASTNAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_E_MAIL)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_PASSWORD)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.USER_IS_ACTIVE_PROFILE))
                )
            )
        }

        cursor.close()

        return users
    }

}