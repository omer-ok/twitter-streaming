package com.omer.twitter_streaming

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterLoginButton
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.tweetui.TweetUtils
import com.twitter.sdk.android.tweetui.TweetView


class TweetDetailsActivity : AppCompatActivity() {
    var loginButton: TwitterLoginButton? = null
    var logoutButton: Button? = null
    var loginToLikeMsgTV: TextView? = null

    // launch the login activity when a guest user tries to favorite a Tweet
    val actionCallback: Callback<Tweet?> = object : Callback<Tweet?>() {
        override fun success(result: Result<Tweet?>) {
            // Intentionally blank
        }

        override fun failure(exception: TwitterException) {}
    }

     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = TwitterConfig.Builder(this)
            .logger(DefaultLogger(Log.DEBUG))
            .twitterAuthConfig(
                TwitterAuthConfig(
                    getResources().getString(R.string.twitter_api_key),
                    getResources().getString(R.string.twitter_api_secret)
                )
            )
            .debug(true)
            .build()
        Twitter.initialize(config)
        setContentView(R.layout.activity_tweet_details)
        loginButton = findViewById(R.id.login_button) as TwitterLoginButton?
        logoutButton = findViewById(R.id.logout_button) as Button?
        loginToLikeMsgTV = findViewById(R.id.tv_login_to_like) as TextView?
        updateLoginUI()
        val myLayout = findViewById(R.id.tweet_details) as LinearLayout
        val bundle: Bundle = getIntent().getExtras()!!
        val tweetId = bundle.getLong("tweetID")
        TweetUtils.loadTweet(tweetId, object : Callback<Tweet?>() {
            override fun success(result: Result<Tweet?>) {
                val tweetView = TweetView(this@TweetDetailsActivity, result.data)
                tweetView.setTweetActionsEnabled(true)
                tweetView.setOnActionCallback(actionCallback)
                myLayout.addView(tweetView)
            }

            override fun failure(exception: TwitterException) {}
        })
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result to the login button.
        loginButton!!.onActivityResult(requestCode, resultCode, data)
        updateLoginUI()
    }

    fun logoutUser(target: View?) {
        CookieSyncManager.createInstance(this)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookie()
        TwitterCore.getInstance().sessionManager.clearActiveSession()
        updateLoginUI()
    }

    private fun updateLoginUI() {
        val session = TwitterCore.getInstance().sessionManager.activeSession
        if (session == null) {
            loginButton!!.visibility = View.VISIBLE
            loginToLikeMsgTV!!.visibility = View.VISIBLE
            logoutButton!!.visibility = View.GONE
            loginButton!!.callback = object : Callback<TwitterSession?>() {
                override fun success(result: Result<TwitterSession?>) {
                    // Do something with result, which provides a TwitterSession for making API calls
                }

                override fun failure(exception: TwitterException) {
                    // Do something on failure
                }
            }
        } else {
            loginButton!!.visibility = View.GONE
            loginToLikeMsgTV!!.visibility = View.GONE
            logoutButton!!.visibility = View.VISIBLE
        }
    }
}