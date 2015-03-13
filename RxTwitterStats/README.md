This is a demo using Play, RxScala and the WidgetManager.
It will retrieve tweets in real-time corresponding to the keyword choosen by the user.

Have a look at the demo video :
[![RxTwitterStats demo](http://img.youtube.com/vi/G70KP-A3AT8/1.jpg)](http://www.youtube.com/watch?v=G70KP-A3AT8)

To be able to run it, you need :

- [sbt](http://www.scala-sbt.org/0.13.2/docs/Getting-Started/Setup.html) or [play](http://www.playframework.com/documentation/2.0/Installing) installed and runnable from the command line.
- to create a file passwords.conf in conf/ folder. It must contains your tokens generated on [Twitter dev page](https://dev.twitter.com/apps) following that format :

```
# Twitter access tokens and keys generated on https://dev.twitter.com/apps/

twitter.accessToken=""
twitter.accessSecret=""
twitter.consumerKey=""
twitter.consumerSecret=""
```
