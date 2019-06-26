
MongoDB Mobile Capacitor Plugin
===============================

MongoDB is one of the easiest databases to use for small javascript-based projects, so it is 
a fantastic fit for many capacitor apps. The goal of this project is to provide a full-featured
interface to allow using MongoDB Mobile from a capacitor app.

This project does not atttempt to clean up the interface and all documents sent are
in MongoDB's canonical Extended JSON format. For those who don't undrestand what that means,
a MongoDB document may look like this:

    {
      _id: ObjectId("5d12cabe21c6fa8a7deb6b9d"),
      name: "This is a cool thing",
      number: 127,
      date: Date("2019-05-13 17:33:22")
    }

Since the above document can't be represented accurately using json, and all communication with
cordova plugins is done using json, we use the extended json format and it looks like this:

    {
      _id: {$oid: "5d12cabe21c6fa8a7deb6b9d"},
      name: "This is a cool thing",
      number: {$numberInt: "127"},
      date: {$date: {$numberLong: "1557790402"}}
    }

In general when you pass things into the plugin to save in the database it'll do its best
to figure out what you meant even if you didn't use the full canonical format, but things like
ObjectIds must be passed as an {$oid: "..."} or it will just be treated as a string.

MongoDB Mobile Client
=====================

Because dealing with the above is a pain (and also because the interface forced on us by having
to do everything with a single layer API is awkward) we have created a project which provides
a wrapper around this plugin which provides an interface *very* similar to the 
[node-native-mongodb](http://mongodb.github.io/node-mongodb-native/3.2/) driver API.

For more information, see the [mongodb-mobile-client](https://github.com/HamStudy/mongodb-mobile-client)
project.