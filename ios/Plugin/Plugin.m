#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(MongoDBMobile, "MongoDBMobile",
           CAP_PLUGIN_METHOD(initWithId, CAPPluginReturnNone);
           CAP_PLUGIN_METHOD(listDatabases, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(count, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(find, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(aggregate, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(cursorGetNext, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(closeCursor, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(insertOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(insertMany, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(replaceOne, CAPPluginReturnPromise);
)
