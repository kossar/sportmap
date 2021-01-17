package ee.taltech.sportmap

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import ee.taltech.sportmap.domain.User
import ee.taltech.sportmap.helpers.CheckNetwork
import ee.taltech.sportmap.repository.UserRepository
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.log_in_layout.*
import kotlinx.android.synthetic.main.log_in_layout.view.*
import kotlinx.android.synthetic.main.register_layout.*
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }
    private val userName: String? = null
    private var user: User? = null
    private lateinit var userRepo: UserRepository

    private var newAccountForm: AlertDialog.Builder? = null

    private var loginEmail = ""
    private var loginPW = ""

    private var loginForm: AlertDialog? = null

    private var jwt: String? = null

    private var isLoggoingIn: Boolean = false
    private var isRegistering: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)
        userRepo = UserRepository(this).open()
        if (savedInstanceState != null) {
            isLoggoingIn = savedInstanceState.getBoolean("isLoggingIn")
        }
        if (!isLoggoingIn) {
            setActiveUser()
        }


    }

    fun buttonNewActivityOnCLick(view: View) {

        val intent  = Intent(this, MapsActivity::class.java)
        if (user != null) {
            intent.putExtra(C.LOCAL_USER_ID, user!!.id)
            startActivity(intent)
        }else {
            Toast.makeText(this, "Please Register or log in..", Toast.LENGTH_SHORT).show()
        }


    }

    fun buttonCompassOnClick(view: View) {
        val intent = Intent(this, CompassActivity::class.java)
        startActivity(intent)
    }

    fun buttonLogInOutOnClick(view: View) {
       logInOut()
    }
    private fun logInOut() {
        //If user == null then log out and reset
        if (user != null) {
            reset()
        }else{

            loginForm = logInFormDialog().create()
            loginForm!!.show()
            isLoggoingIn = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userRepo.close()
        if (loginForm != null) {
            loginForm!!.dismiss()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")

        Log.d(TAG, loginPW)

        if (isLoggoingIn) {
            outState.putBoolean("isLoggingIn", isLoggoingIn)
            outState.putString("eMail", loginEmail)
            outState.putString("password", loginPW)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState")
        isLoggoingIn = savedInstanceState.getBoolean("isLoggingIn")
        if (isLoggoingIn){
            loginEmail = savedInstanceState.getString("eMail").toString()
            loginPW = savedInstanceState.getString("password").toString()
            loginForm = logInFormDialog().create()
            loginForm!!.show()
        }

    }



    fun buttonNewAccountOnClick(view: View) {
        newAccountForm = newAccountFormDialog()
        newAccountForm!!.show()
    }

    private fun logInFormDialog(): AlertDialog.Builder {
        val inflater = LayoutInflater.from(this)
        val logIn = inflater.inflate(R.layout.log_in_layout, null)

        val loginDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        loginDialog.setTitle(R.string.log_in)
        loginDialog.setMessage(R.string.log_in_dialog_msg)
        loginDialog.setView(logIn)
        val eMailEditText = logIn.findViewById<EditText>(R.id.editTextTextEmailAddressLogIn)
        val pwEditText = logIn.findViewById<EditText>(R.id.editTextTextPasswordLogIn)
       eMailEditText.addTextChangedListener {
           loginEmail = it.toString()
       }
        pwEditText.addTextChangedListener {
            loginPW = it.toString()
        }

        eMailEditText.setText(loginEmail)
        pwEditText.setText(loginPW)

        loginDialog.setPositiveButton(
            R.string.log_in,
            DialogInterface.OnClickListener { dialog, whichButton ->
                loginEmail = eMailEditText.text.trim().toString()
                loginPW = pwEditText.text.trim().toString()

                userLogInHandler()
                isLoggoingIn = false
                dialog.dismiss()
                return@OnClickListener
            })

        loginDialog.setNegativeButton(R.string.cancel,
            DialogInterface.OnClickListener { dialog, which -> // TODO Auto-generated method stub
                isLoggoingIn = false
                return@OnClickListener

            })


        return loginDialog
    }

    private fun newAccountFormDialog(): AlertDialog.Builder {
        val inflater = LayoutInflater.from(this)
        val register = inflater.inflate(R.layout.register_layout, null)
        val alert: AlertDialog.Builder = AlertDialog.Builder(this)

        alert.setTitle(R.string.register_new_account)
        alert.setMessage(R.string.register_new_account_instruction)
        alert.setView(register)
        val first = register.findViewById<EditText>(R.id.editTextUserFirstNameRegister)
        var last = register.findViewById<EditText>(R.id.editTextUserLastNameRegister)
        var email = register.findViewById<EditText>(R.id.editTextEmailAddressRegister)
        var pw = register.findViewById<EditText>(R.id.editTextPasswordRegister)

        alert.setPositiveButton(R.string.register, DialogInterface.OnClickListener { dialog, _ ->
            user = User(
                first.text.trim().toString(),
                last.text.trim().toString(),
                email.text.trim().toString(),
                pw.text.trim().toString()
            )
            Log.d(TAG, user.toString())
            if (CheckNetwork.isNetworkAvailable(this)) {
                getRestToken(true)
            }

            dialog.dismiss()
            return@OnClickListener
        })

        alert.setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
            return@OnClickListener
        })
        alert.create()
        return alert
    }

    private fun getRestToken(fromRegistration: Boolean) {
        Log.d(TAG, "getRestToken")

        var handler = WebApiSingletonHandler.getInstance(applicationContext)

        val requestJsonParameters = JSONObject()

        var url = C.REST_LOGIN
        if (!fromRegistration) {
            requestJsonParameters.put("email", loginEmail)
            requestJsonParameters.put("password", loginPW)
        }
        if (fromRegistration) {
            url = C.REST_REGISTER_ACCOUNT
            requestJsonParameters.put("email", user!!.eMail)
            requestJsonParameters.put("password", user!!.password)
            requestJsonParameters.put("firstName", user!!.firstName)
            requestJsonParameters.put("lastName", user!!.lastName)
        }

        var httpRequest = JsonObjectRequest(
            Request.Method.POST,
            url,
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, "Response: " + response.toString())
                jwt = response.getString("token")
                if (!fromRegistration) {
                    user = User(
                        response.getString("firstName"),
                        response.getString("lastName"),
                        loginEmail!!,
                        loginPW!!
                    )
                }
                Log.d(TAG, user.toString())
                userFromApiSuccessHandler()
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
            }
        )

        handler.addToRequestQueue(httpRequest)

    }


    private fun userLogInHandler() {
        Log.d(TAG, "userLogInHandler")
        if (loginEmail != "" && loginPW != "") {
            // First try to find user from local db
            user = userRepo.userLogIn(loginEmail!!, loginPW!!)
            Log.d(TAG, user.toString())
            // If user is not find in local db, then try to find it from API
            if (user == null) {
                getRestToken(false)
            }

            if (user != null) {
                setCurrentUserActive()
                setUp()
            }
        }
        Log.d(TAG, "Current user: " + user.toString())
    }

    private fun setCurrentUserActive() {
        if (user!!.isActiveProfile == 0) {
            userRepo.deActivateUser()
            user!!.isActiveProfile = 1
            userRepo.updateUser(user!!)
        }
    }
    private fun setActiveUser() {
        if (userRepo.hasActiveUser()) {
            user = userRepo.getActiveUser()
           setUp()
        }
    }
    private fun userFromApiSuccessHandler() {
        // Set user active after successful registration
        Log.d(TAG, "userFromApiSuccessHandler")
        user!!.isActiveProfile = 1
        userRepo.deActivateUser()
        user!!.id = userRepo.saveUser(user!!)
        setUp()
    }

    private fun setUp() {
        buttonLogInOut.setText(R.string.log_out)
        textViewWelcome.text = "Welcome ${user!!.firstName} ${user!!.lastName}"
    }
    private fun reset(){
        user = null
        buttonLogInOut.setText(R.string.log_in)
        jwt = null
        textViewWelcome.setText(R.string.not_logged_in)
        userRepo.deActivateUser()

    }

    fun buttonHistoryOnClick(view: View) {
        if (user != null) {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("userId", user!!.id)
            startActivity(intent)
        }

    }


}