package ee.taltech.sportmap.helpers

import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.util.concurrent.TimeUnit

class LSHelper {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private var mapPolylineOptions: PolylineOptions? = null

        @Synchronized
        fun getMapPolylineOptions(): PolylineOptions {
            if (mapPolylineOptions == null) {
                mapPolylineOptions = PolylineOptions().width(10f)
                        .color(Color.RED)
            }
            return mapPolylineOptions!!
        }

        fun clearMapPolylineOptions() {
            mapPolylineOptions = PolylineOptions().width(10f)
                    .color(Color.RED)
        }

        fun addToMapPolylineOptions(lat: Double, lon: Double) {
            getMapPolylineOptions().add(LatLng(lat, lon))
        }

        fun getTimeString(millis: Long): String {
            return String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(millis)
                ),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(millis)
                )
            )
        }


        fun getPace(millis: Long, distance: Float): String {
            Log.d(TAG, millis.toString() + '-' + distance.toString())
            val speed = millis / 60.0 / distance
            if (speed > 99) return "--:--"
            val minutes = (speed ).toInt();
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes.toString() + ":" + (if (seconds < 10)  "0" else "") +seconds.toString();

        }


    }
}