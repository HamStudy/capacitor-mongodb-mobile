package org.hamstudy.MongoDb;

import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// Base Stitch Packages
import com.mongodb.WriteConcern;
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

    HashMap<UUID, MongoCursor<Document>> cursorMap = new HashMap<UUID, MongoCursor<Document>>();

    @PluginMethod()
    public void initDb(PluginCall call) {
        final StitchAppClient client =
                Stitch.initializeDefaultAppClient(getBridge().getActivity().getPackageName());

        mongoClient = client.getServiceClient(LocalMongoDbService.clientFactory);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.success(ret);
    }

    private MongoDatabase getDatabase(PluginCall call) throws InvalidParameterException {
        return getDatabase(call, "options");
    }
    private MongoDatabase getDatabase(PluginCall call, String optionsKey) throws InvalidParameterException {
        String dbName = call.getString("db", "");
        if (dbName.isEmpty()) {
            throw new InvalidParameterException("db name must be provided and must be a string");
        }
        MongoDatabase db = mongoClient.getDatabase(dbName);

        if (!optionsKey.isEmpty() && call.hasOption(optionsKey)) {
            JSObject options = call.getObject(optionsKey);
            try {
                WriteConcern wc = OptionParser.getWriteConcern(options, "writeConcern");
                db = db.withWriteConcern(wc);
            } catch (Exception ex) {}
        }
        return db;
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

    /**
     * Helper to return a cursor to the page
     * @param call
     * @param cursor
     */
    private void returnCursor(PluginCall call, MongoCursor<Document> cursor) {

        UUID cursorId = UUID.randomUUID();

        cursorMap.put(cursorId, cursor);

        JSObject ret = new JSObject();
        ret.put("cursorId", cursorId.toString());
        call.resolve(ret);
    }

    /**
     * Helper to return an array of documents to the page
     * @param call
     * @param cursor
     */
    private void returnDocsFromCursor(PluginCall call, MongoCursor<Document> cursor) {
        JSONArray resultsJson = new JSONArray();

        while (cursor.hasNext()) {
            Document cur = cursor.next();
            try {
                resultsJson.put(new JSONObject(cur.toJson(jsonSettings)));
            } catch (Exception ex) {
                // This shouldn't be possible, in theory, but who knows?
                handleError(call, ex.toString(), ex);
                return;
            }
        }
        JSObject ret = new JSObject();
        ret.put("results", resultsJson);
        call.resolve(ret);
    }

    /**
     * Helper to return an array of documents to the page which returns them as base64-encoded
     * BSON documents
     * @param call
     * @param cursor
     */
    private void returnDocsFromCursorBson(PluginCall call, MongoCursor<RawBsonDocument> cursor) {
        JSONArray resultsJson = new JSONArray();

        while (cursor.hasNext()) {
            RawBsonDocument cur = cursor.next();
            ByteBuf data = cur.getByteBuffer();
            String b64String = Base64.encodeToString(data.array(), Base64.DEFAULT);

            JSONObject obj = new JSONObject();
            try {
                obj.put("$b64", b64String);
            } catch (Exception ex) {
                handleError(call, "Could not wrap base64 string", ex);
                return;
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
                resultsJson.put(new JSONObject(cur.toJson(jsonSettings)));
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
            ArrayList<String> names = mongoClient.listDatabaseNames().into(new ArrayList<String>());

            JSObject ret = new JSObject();
            if (names.contains(dbName)) {
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
            MongoDatabase db = getDatabase(call);
            MongoCursor<Document> collections = db.listCollections().iterator();

            JSONArray resultsJson = new JSONArray();
            while (collections.hasNext()) {
                Document cur = collections.next();
                resultsJson.put(new JSONObject(cur.toJson(jsonSettings)));
            }

            JSObject ret = new JSObject();
            ret.put("collections", resultsJson);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute listCollections", ex);
        }
    }

    @PluginMethod()
    public void createCollection(PluginCall call) {
        try { String collectionName = call.getString("collection", "");
            if (collectionName.isEmpty()) {
                throw new InvalidParameterException("collection name must be provided and must be a string");
            }

            MongoDatabase db = getDatabase(call);
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
    @PluginMethod()
    public void dropCollection(PluginCall call) {
        try {
            String collectionName = call.getString("collection", "");
            if (collectionName.isEmpty()) {
                throw new InvalidParameterException("collection name must be provided and must be a string");
            }

            MongoDatabase db = getDatabase(call);

            ArrayList<String> names = mongoClient.listDatabaseNames().into(new ArrayList<String>());
            JSObject ret = new JSObject();
            if (names.contains(collectionName)) {
                MongoCollection collection = db.getCollection(collectionName);
                collection.drop();
                ret.put("dropped", true);
            } else {
                ret.put("dropped", false);
            }

            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }

    @PluginMethod()
    public void runCommand(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);

            JSObject commandSrc = call.getObject("command");
            if (commandSrc == null) {
                throw new InvalidParameterException("command must be a valid document");
            }
            JSObject rootObj = new JSObject();
            rootObj.put("command", commandSrc);
            Document command = OptionParser.getDocument(rootObj, "command");
            Document reply = db.runCommand(command);

            JSObject ret = new JSObject();
            ret.put("reply", new JSONObject(command.toJson(jsonSettings)));

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropDatabase", ex);
        }
    }
}
