package ee.taltech.sportmap.helpers

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService

class CheckNetwork {
    companion object {
         fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

}