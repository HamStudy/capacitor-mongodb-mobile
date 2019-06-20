#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(MongoDBMobile, "MongoDBMobile",
           CAP_PLUGIN_METHOD(initWithId, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(listDatabases, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(count, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(find, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(aggregate, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(cursorGetNext, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(closeCursor, CAPPluginReturnPromise);
           

           CAP_PLUGIN_METHOD(insertOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(insertMany, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(replaceOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(updateOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(updateMany, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(deleteOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(deleteMany, CAPPluginReturnPromise);

           
           CAP_PLUGIN_METHOD(findOneAndDelete, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(findOneAndReplace, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(findOneAndUpdate, CAPPluginReturnPromise);

           CAP_PLUGIN_METHOD(newBulkWrite, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddDeleteOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddDeleteMany, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddInsertOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddReplaceOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddUpdateOne, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteAddUpdateMany, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bulkWriteExecute, CAPPluginReturnPromise);

           CAP_PLUGIN_METHOD(dropIndex, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(createIndexes, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(listIndexes, CAPPluginReturnPromise);

           
)
