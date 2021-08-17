# Simple Android app for showing realtime Tweets around user's location on Google Map

## Highlights

1. MVVM Architectural pattern
2. Use of Twitter4J (To stream realtime tweets around the user)
3. Use of Twitter Kit Android (For twitter login and showing tweet)
4. Show Realtime tweets inside specific radius 
5. Tweet search using Keyword or Hashtag
6. Multi language support (English and French)

## Setup instructions
1. Change Twitter credentials
   1. Generate Twitter API key, API Secret, Access Token and Access Token secret from [HERE](https://developer.twitter.com/en/apps)
   2. Replace them in `res/values/strings.xml` with `YOUR_TWITTER_API_KEY`, `YOUR_TWITTER_API_SECRET`, `YOUR_TWITTER_ACCESS_TOKEN` and `YOUR_TWITTER_ACCESS_TOKEN_SECRET` respectively
2. Change Google Map credentials
    1. Generate Google Map API Key for android from [Google API Console](https://console.developers.google.com/apis/dashboard)
    2. Replace `YOUR_GOOGLE_API_KEY` with your API Key in AndroidManifest.xml file.
3. That's It!

## ToDos
1. Implement Re-tweet functionality
2. Implement dependency injection (Dagger2)
3. Implement DataBinding
4. Add more language support

## Screenshots
<img src="/screenshots/tweets_on_map_en.png" width="346" height="615" alt="Realtime Tweets on Map"/> 
<img src="/screenshots/tweet_details.png" width="346" height="615" alt="Tweet Detail with Twitter Login button"/>
<img src="/screenshots/tweet_details_loggedin.png" width="346" height="615" alt="Tweet Detail with Logout button"/>
<img src="/screenshots/edit_radius.png" width="346" height="615" alt="Edit radious preference"/>
<img src="/screenshots/tweets_on_map_fr.png" width="346" height="615" alt="Realtime Tweets on Map - Fr"/>
