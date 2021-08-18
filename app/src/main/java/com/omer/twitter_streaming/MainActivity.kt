package com.omer.twitter_streaming


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.omer.twitter_streaming.viewmodel.TweetsViewModel
import twitter4j.Status
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMarkerClickListener, OnInfoWindowClickListener {
    var lastKnownLocation: Location? = null
    var searchTerm = ""
    var radius = 5.0
    var locationManager: LocationManager? = null
    private var mMap: GoogleMap? = null
    var locationProvider = LocationManager.GPS_PROVIDER
    var markersWeakHashMap = HashMap<Long, Marker>()
    var tweetsWeakHashMap = HashMap<Long, Status>()
    var oldTweets: MutableList<Status> = ArrayList()
    var listLiveData: LiveData<List<Status>>? = null
    var tweetsViewModel: TweetsViewModel? = null
    var radiusTV: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = getSupportFragmentManager()
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        radiusTV = findViewById(R.id.radius_tv) as TextView?
        radiusTV!!.text = " $radius KM"
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                APP_PERMISSIONS_REQUEST_GET_LOCATION
            )
            return
        }
        lastKnownLocation = locationManager!!.getLastKnownLocation(locationProvider)
        tweetsViewModel = ViewModelProviders.of(this).get(TweetsViewModel::class.java)
        if(lastKnownLocation!=null){
            listLiveData = tweetsViewModel!!.startStreaming(
                this@MainActivity,
                lastKnownLocation!!,
                searchTerm,
                radius
            )
        }
        startObserving()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val searchView: SearchView
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.search_menu, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.action_search)
            .actionView as SearchView
        searchView.setSearchableInfo(
            searchManager
                .getSearchableInfo(getComponentName())
        )
        searchView.setMaxWidth(Int.MAX_VALUE)

        // listening to search query text change
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchTerm = query.trim { it <= ' ' }
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    tweetsViewModel?.stopStreaming()
                    if(lastKnownLocation!=null){
                        listLiveData = tweetsViewModel!!.startStreaming(
                            getApplicationContext(),
                            lastKnownLocation!!,
                            searchTerm,
                            radius
                        )
                    }
                }
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
        })
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun startObserving() {
        listLiveData!!.observe(this, androidx.lifecycle.Observer { tweets:List<Status> ->
            val newTweets: MutableList<Status> =
                ArrayList()
            for (tweet in tweets) {
                newTweets.add(tweet)
            }
            val common: MutableList<Status> =
                ArrayList(oldTweets)
            common.retainAll(newTweets)
            val tweetsToAdd: MutableList<Status> =
                ArrayList(newTweets)
            tweetsToAdd.removeAll(common)
            val tweetsToRemove: MutableList<Status> =
                ArrayList(oldTweets)
            tweetsToRemove.removeAll(common)
            if (tweets.size == 0) {
                //clear map
                mMap!!.clear()
            } else {
                for (newTweet in tweetsToAdd) {
                    oldTweets.add(newTweet)
                    val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    )
                    val tweetLocation = LatLng(
                        newTweet.geoLocation.latitude,
                        newTweet.geoLocation.longitude
                    )
                    val newlyAddedMarker =
                        mMap!!.addMarker(
                            MarkerOptions().position(tweetLocation)
                                .icon(bitmapDescriptor)
                                .title("Tweet my @" + newTweet.user.screenName)
                        )
                    newlyAddedMarker.tag = newTweet.id
                    markersWeakHashMap[newTweet.id] = newlyAddedMarker
                    tweetsWeakHashMap[newTweet.id] = newTweet
                    newlyAddedMarker.showInfoWindow()
                }
                for (tweetToRemove in tweetsToRemove) {
                    val markerToRemove =
                        markersWeakHashMap[tweetToRemove.id]
                    markerToRemove!!.remove()
                    markersWeakHashMap.remove(tweetToRemove.id)
                    tweetsWeakHashMap.remove(tweetToRemove.id)
                    oldTweets.remove(tweetToRemove)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            APP_PERMISSIONS_REQUEST_GET_LOCATION -> {
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted, yay!
                    lastKnownLocation = locationManager!!.getLastKnownLocation(locationProvider)
                    tweetsViewModel = ViewModelProviders.of(this).get(TweetsViewModel::class.java)
                    if(lastKnownLocation!=null){
                        listLiveData = tweetsViewModel?.startStreaming(
                            getApplicationContext(),
                            lastKnownLocation!!,
                            searchTerm,
                            radius
                        )
                    }
                    startObserving()
                    updateLocationUI()
                } else {
                    // permission denied, boo!
                    val snackbar: Snackbar = Snackbar
                        .make(
                            findViewById(R.id.mainConstraintLayout),
                            getResources().getString(R.string.message_need_location_permission),
                            Snackbar.LENGTH_LONG
                        )
                    snackbar.show()
                }
                return
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (lastKnownLocation != null) {
            val userLocation = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            val cameraPosition = CameraPosition.Builder()
                .target(userLocation) // Sets the center of the map to Mountain View
                .zoom(10f) // Sets the zoom
                .bearing(90f) // Sets the orientation of the camera to east
                .tilt(30f) // Sets the tilt of the camera to 30 degrees
                .build() // Creates a CameraPosition from the builder
            mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            mMap!!.isMyLocationEnabled = true
            mMap!!.uiSettings.isMyLocationButtonEnabled = true
            mMap!!.setInfoWindowAdapter(CustomInfoWindowAdapter())
            mMap!!.setOnMarkerClickListener(this)
            mMap!!.setOnInfoWindowClickListener(this)
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        val intent = Intent(this, TweetDetailsActivity::class.java)
        intent.putExtra("tweetID", marker.tag as Long?)
        startActivity(intent)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return true
    }

    inner class CustomInfoWindowAdapter internal constructor() : InfoWindowAdapter {
        private val mContents: View
        override fun getInfoWindow(marker: Marker): View? {
            return null
        }

        override fun getInfoContents(marker: Marker): View {
            val tweet = tweetsWeakHashMap[marker.tag]
            if (tweet != null) {
                val tvUserDisplayName =
                    mContents.findViewById<View>(R.id.userDisplayName) as TextView
                tvUserDisplayName.text = "@" + tweet.user.screenName
                val tvTweetGist = mContents.findViewById<View>(R.id.tweetGist) as TextView
                tvTweetGist.text = tweet.text
            }
            return mContents
        }

        init {
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null)
        }
    }

    fun changeRadius(target: View?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getResources().getString(R.string.radius_in_km))
        // I'm using fragment here so I'm using getView() to provide ViewGroup
// but you can provide here any other instance of ViewGroup from your Fragment / Activity
        val viewInflated: View =
            LayoutInflater.from(this).inflate(R.layout.text_input_redius, null, false)
        // Set up the input
        val input = viewInflated.findViewById<View>(R.id.input) as EditText
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        builder.setView(viewInflated)

// Set up the buttons
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            dialog.dismiss()
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if(input.text.trim().toString()!=""){
                    radius = input.text.trim().toString().toLong().toDouble()
                    radiusTV!!.text = " $radius KM"
                    tweetsViewModel?.stopStreaming()
                    if(lastKnownLocation!=null){
                        listLiveData = tweetsViewModel?.startStreaming(
                            getApplicationContext(),
                            lastKnownLocation!!,
                            searchTerm,
                            radius
                        )
                    }
                }else{
                    Toast.makeText(this, "Please Select a radius ", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val APP_PERMISSIONS_REQUEST_GET_LOCATION = 1
    }
}
