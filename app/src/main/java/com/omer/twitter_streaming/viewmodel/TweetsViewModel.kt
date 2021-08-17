package com.omer.twitter_streaming.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.omer.twitter_streaming.R
import com.omer.twitter_streaming.utilz.LocationHelper
import twitter4j.*
import twitter4j.conf.ConfigurationBuilder
import java.util.*


class TweetsViewModel : ViewModel() {
    private var tweets: MutableLiveData<List<Status>>? = null
    fun startStreaming(
        context: Context,
        lastKnownLocation: Location,
        searchTerm: String,
        radius: Double
    ): LiveData<List<Status>> {
        if (tweets == null) {
            tweets = MutableLiveData()
            val cb = ConfigurationBuilder()
            cb.setDebugEnabled(true)
            cb.setOAuthConsumerKey(context.resources.getString(R.string.twitter_api_key))
            cb.setOAuthConsumerSecret(context.resources.getString(R.string.twitter_api_secret))
            cb.setOAuthAccessToken(context.resources.getString(R.string.twitter_access_token))
            cb.setOAuthAccessTokenSecret(context.resources.getString(R.string.twitter_access_token_secret))
            val twitterStream = TwitterStreamFactory(cb.build()).instance
            val listener: StatusListener = object : StatusListener {
                override fun onException(ex: Exception) {}
                override fun onStatus(status: Status) {
                    val user = status.user
                    // gets Username
                    if (status.geoLocation != null) {
                        val tempStatusList: MutableList<Status> =
                            if (tweets!!.value == null) ArrayList() else tweets!!.getValue() as MutableList<Status>
                        tempStatusList.add(status)
                        //If there are more than 100 tweets in list then remove old one
                        if (tempStatusList.size > 100) {
                            tempStatusList.removeAt(0)
                        }
                        tweets!!.postValue(tempStatusList)
                    }
                }

                override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
                override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
                override fun onScrubGeo(userId: Long, upToStatusId: Long) {}
                override fun onStallWarning(warning: StallWarning) {}
            }
            val tweetFilterQuery = FilterQuery()
            val locationHelper:LocationHelper
            locationHelper = LocationHelper()
            val locationBounds: LatLngBounds = locationHelper.toBounds(
                LatLng(
                    lastKnownLocation.latitude,
                    lastKnownLocation.longitude
                ), radius * 1000
            )
            tweetFilterQuery.locations(
                *arrayOf(
                    doubleArrayOf(
                        locationBounds.southwest.longitude,
                        locationBounds.southwest.latitude
                    ),
                    doubleArrayOf(
                        locationBounds.northeast.longitude,
                        locationBounds.northeast.latitude
                    )
                )
            )
            val keywords = arrayOf(searchTerm)
            tweetFilterQuery.track(*keywords)
            twitterStream.addListener(listener)
            twitterStream.filter(tweetFilterQuery)
        }
        return tweets!!
    }

    fun stopStreaming(): LiveData<List<Status>?>? {
        tweets!!.postValue(ArrayList())
        return tweets
    }

    companion object {
        private const val TAG = "TweetsViewModel"
    }
}
