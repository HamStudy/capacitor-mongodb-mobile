package org.hamstudy.MongoDb;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.security.InvalidParameterException;
import java.util.UUID;

// Base Stitch Packages
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;

// Packages needed to interact with MongoDB and Stitch
import com.mongodb.client.MongoClient;

// Necessary component for working with MongoDB Mobile
import com.mongodb.stitch.android.services.mongodb.local.LocalMongoDbService;

import org.bson.ByteBuf;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.hamstudy.capacitor.MongoDb.OptionParser;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.Dictionary;

@NativePlugin()
public class MongoDBMobile extends Plugin {

    private JsonWriterSettings jsonSettings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    // Create a Client for MongoDB Mobile (initializing MongoDB Mobile)
    MongoClient mongoClient;

    Dictionary<UUID, MongoCursor<Document>> cursorMap = new Map<UUID, MongoCursor<Document>>();

    @PluginMethod()
    public void initDb(PluginCall call) {
        final StitchAppClient client =
                Stitch.initializeDefaultAppClient(getBridge().getActivity().getPackageName());

        mobileClient = client.getServiceClient(LocalMongoDbService.clientFactory);

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    /**
     * Helper for handling errors
     */
    private void handleError(PluginCall call, String message) {
        handleError(call, message, null);
    }
    private void handleError(PluginCall call, String message, Exception error) {
        call.error(message, error);
    }

    private void returnCursor(PluginCall call, MongoCursor<Document> cursor) {

        UUID cursorId = UUID.randomUUID();

        cursorMap.put(cursorId, cursor);

        JSObject ret = new JSObject();
        ret.put("cursorId", cursorId.toString());
        call.resolve(ret);
    }
    private void returnDocsFromCursor(PluginCall call, MongoCursor<Document> cursor) {
        JSONArray resultsJson = new JSONArray();

        while (cursor.hasNext()) {
            Document cur = cursor.next();
            resultsJson.put(cur.toJson(jsonSettings));
        }
        JSObject ret = new JSObject();
        ret.put("results", resultsJson);
        call.resolve(ret);
    }
    private void returnDocsFromCursorBson(PluginCall call, MongoCursor<RawBsonDocument> cursor) {
        JSONArray resultsJson = new JSONArray();

        while (cursor.hasNext()) {
            RawBsonDocument cur = cursor.next();
            ByteBuf data = cur.getByteBuffer();
            String b64String = Base64.encode(data);

            JSONObject obj = new JSONObject();
            try {
                obj.put("$b64", b64String);
            } catch (Exception ex) {
                handleError(call, "Could not wrap base64 string for some reason", ex);
            }

            resultsJson.put(obj);
        }
        JSObject ret = new JSObject();
        ret.put("results", resultsJson);
        call.resolve(ret);
    }

    /** DATABASE METHODS **/
    @PluginMethod()
    public void listDatabases(PluginCall call) {
        try {
            ListDatabasesIterable<Document> list = mongoClient.listDatabases();
            MongoCursor<Document> cursor = list.iterator();

            JSONArray resultsJson = new JSONArray();
            while (cursor.hasNext()) {
                Document cur = cursor.next();
                resultsJson.put(cur.toJson(jsonSettings));
            }
            JSObject ret = new JSObject();
            ret.put("databases", resultsJson);
            call.resolve(ret);
        } catch (Exception ex) {
            handleError(call, "Could not list databases!", ex);
        }
    }
    @PluginMethod()
    public void dropDatabase(PluginCall call) {
        try {
            String dbName = call.getString("db", "");
            if (dbName.isEmpty()) {
                throw new InvalidParameterException("db name must be provided and must be a string");
            }
            List<String> names = mongoClient.listDatabaseNames().into(new List<String>());

            JSObject ret = new JSObject();
            if (names.stream().anyMatch(str -> str.equals(dbName))) {
                MongoDatabase db = mongoClient.getDatabase(dbName);
                db.drop();
                ret.put("dropped", true);
            } else {
                ret.put("dropped", false);
            }
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropDatabase", ex);
        }
    }


    @PluginMethod()
    public void listCollections(PluginCall call) {
        try {
            String dbName = call.getString("db", "");
            if (dbName.isEmpty()) {
                throw new InvalidParameterException("db name must be provided and must be a string");
            }

            MongoDatabase db = mongoClient.getDatabase(dbName);
            MongoCursor<Document> collections = db.listCollections().iterator();

            JSONArray resultsJson = new JSONArray();
            while (cursor.hasNext()) {
                Document cur = cursor.next();
                resultsJson.put(cur.toJson(jsonSettings));
            }

            JSObject ret = new JSObject();
            ret.put("collections", resultsJson);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropDatabase", ex);
        }
    }

    @PluginMethod()
    public void createCollection(PluginCall call) {
        try {
            String dbName = call.getString("db", "");
            if (dbName.isEmpty()) {
                throw new InvalidParameterException("db name must be provided and must be a string");
            }
            String collectionName = call.getString("collection", "");
            if (collectionName.isEmpty()) {
                throw new InvalidParameterException("collection name must be provided and must be a string");
            }

            MongoDatabase db = mongoClient.getDatabase(dbName);
            CreateCollectionOptions opts = OptionParser.getCreateCollectionOptions(call.getObject("options", new JSObject()));

            db.createCollection(collectionName, opts);

            JSObject ret = new JSObject();
            ret.put("collection", collectionName);

            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropDatabase", ex);
        }
    }
//    @PluginMethod()
//    public void dropCollection(PluginCall call) {
//        try {
//            guard let dbName = call.getString("db") else {
//                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
//            }
//            guard let collectionName = call.getString("collection") else {
//                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
//            }
//            let db = mongoClient!.db(dbName)
//            let collection = db.collection(collectionName)
//            try collection.drop()
//
//            let data: PluginResultData = [
//            "dropped": true
//            ]
//            call.resolve(data)
//        } catch (InvalidParameterException ex) {
//            handleError(call, ex.getMessage(), ex);
//        } catch (Exception ex) {
//            handleError(call, "Could not execute dropDatabase", ex);
//        }
//    }
//
//    @PluginMethod()
//    public void runCommand(PluginCall call) {
//        try {
//            guard let dbName = call.getString("db") else {
//                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
//            }
//            guard let command = try OptionsParser.getDocument(call.getObject("command"), "command") else {
//                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
//            }
//            let opts = try OptionsParser.getRunCommandOptions(call.getObject("options"))
//
//            let db = mongoClient!.db(dbName)
//
//            let reply = try db.runCommand(command, options: opts)
//
//            let data: PluginResultData = [
//            "reply": convertToDictionary(text: reply.canonicalExtendedJSON)!
//            ]
//
//            call.resolve(data)
//
//        } catch (InvalidParameterException ex) {
//            handleError(call, ex.getMessage(), ex);
//        } catch (Exception ex) {
//            handleError(call, "Could not execute dropDatabase", ex);
//        }
//    }
}
