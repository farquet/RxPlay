This is a demo using Play, RxScala and the WidgetManager.
It will retrieve tweets in real-time corresponding to the keyword choosen by the user.

To be able to run it, you need :

- play or sbt installed and runnable from the command line. [Play installation here](http://www.playframework.com/documentation/2.0/Installing), [Sbt installation here](http://www.scala-sbt.org/0.13.2/docs/Getting-Started/Setup.html)
- to create a file passwords.conf in conf/ folder. It must contains your tokens generated on [Twitter dev page](https://dev.twitter.com/apps) following that format :

```
# Twitter access tokens and keys generated on https://dev.twitter.com/apps/

twitter.accessToken=""
twitter.accessSecret=""
twitter.consumerKey=""
twitter.consumerSecret=""
```