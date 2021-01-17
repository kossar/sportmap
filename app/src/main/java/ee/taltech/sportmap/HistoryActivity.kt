package ee.taltech.sportmap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.sportmap.repository.GPSLocationRepository
import kotlinx.android.synthetic.main.activity_history.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var gpsLocationRepo: GPSLocationRepository

    private lateinit var adapter: RecyclerView.Adapter<*>

    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        if (intent.hasExtra("userId")) {
            userId = intent.getIntExtra("userId", 0)
        }

        gpsLocationRepo = GPSLocationRepository(this).open()

        recyclerViewSessions.layoutManager = LinearLayoutManager(this)
        adapter = DataRecyclerViewAdapter(this, gpsLocationRepo, userId)
        recyclerViewSessions.adapter = adapter
        recyclerViewSessions.isNestedScrollingEnabled = false;
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("userId", userId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        userId = savedInstanceState.getInt("userId")
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsLocationRepo.close()
    }
}