This is a demo using Play, RxScala and the WidgetManager.
It will retrieve tweets in real-time corresponding to the keyword choosen by the user.

To be able to run it, you need :

- play installed and runnable from the command line. [Instructions here](http://www.playframework.com/documentation/2.0/Installing)
- to create a file passwords.conf in conf/ folder. It must contains your tokens generated on [Twitter dev page](https://dev.twitter.com/apps) following that format :

```
# Twitter access tokens and keys generated on https://dev.twitter.com/apps/

twitter.accessToken=""
twitter.accessSecret=""
twitter.consumerKey=""
twitter.consumerSecret=""
```