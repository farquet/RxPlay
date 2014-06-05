RxPlay
======

We develop here how we can efficiently use [RxScala](http://rxscala.github.io) to write reactive apps in [Play framework](http://www.playframework.com). This comes from the observation that Iteratees from Play may be harder to work with than Observables from Rx.

First, we have to deal with the problem that Play uses Iteratees, Enumeratees and Enumerators to handle data reactively. Iteratee is the consumer, Enumeratee the transformer and Enumerators the producer. In Rx, the Observable object has the three roles. Thanks to the work from [Bryan Gilbert](http://bryangilbert.com), we have an implicit mapping between Enumerators and Observables, that we can use store in RxPlay.scala library.

Then, we can see that Iteratees and Enumerators are deeply integrated with the Play framework. Our ultimate goal would be to make them completely disappear from the coder side. This project will illustrate how it can be done in a specific example : WebSockets. Indeed, to deal with them in play you have to create an Iteratee (data consumer) and an Enumerator (data producer) to respectively handle data received from the client side and send them data. If you need to modify the data you send depending (which is often the case), you need complicated dependency between the two objects. If we can work with Observables in that case, this leads to an easier way to code, but with the problem that we cannot respect the Play API. We need to return (in: Iteratee, out: Enumerator). This is what I will now call a WebSocket and what the WidgetManager class will handle to bridge our simple Observables to that structure.

## This repo contains several Play projects

* RxTime : the simplest possible app that could serve as a template project. This is a simple web page with a single button. When you click on it, this creates an Observable on the server that will produce the time every second. You can stop the clock (i. e. unsubscribe from the Observable) and start it again (new subscription).

* RxTwitterStats : this is a complex app illustrating the power of RxPlay combined with WidgetManager (several Observables, chaining them, buffering, etc). This will show real-time tweets for a specific keyword. Top 3 mentionned users and number of tweets per second are displayed on screen. See Readme from the project directory for details and configuration (to add your Twitter tokens).


Project by François Farquet, June 2014.
