package org.hamstudy.MongoDb;

import android.util.Base64;

import com.getcapacitor.JSArray;
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
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;

// Packages needed to interact with MongoDB and Stitch
import com.mongodb.client.MongoClient;

// Necessary component for working with MongoDB Mobile
import com.mongodb.stitch.android.services.mongodb.local.LocalMongoDbService;

import org.bson.BsonValue;
import org.bson.ByteBuf;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.hamstudy.capacitor.MongoDb.OptionParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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
    private MongoCollection<Document> getCollection(PluginCall call, MongoDatabase db) {
        String collectionName = call.getString("db", "");
        if (collectionName.isEmpty()) {
            throw new InvalidParameterException("collection name must be provided and must be a string");
        }
        MongoCollection collection = db.getCollection(collectionName);
        return collection;
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
        JSArray resultsJson = new JSArray();

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
        JSArray resultsJson = new JSArray();

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


    /**********************
     ** DATABASE METHODS **
     **********************/
    @PluginMethod()
    public void listDatabases(PluginCall call) {
        try {
            ListDatabasesIterable<Document> list = mongoClient.listDatabases();
            MongoCursor<Document> cursor = list.iterator();

            JSArray resultsJson = new JSArray();
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

            JSArray resultsJson = new JSArray();
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
            handleError(call, "Could not execute createCollection", ex);
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
            handleError(call, "Could not execute runCommand", ex);
        }
    }


    /******************
     ** READ METHODS **
     ******************/
    @PluginMethod()
    public void count(PluginCall call) {
        try {
            CountOptions opts = OptionParser.getCountOptions(call.getObject("options"));
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }

            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            long count = collection.countDocuments(filterDoc);

            JSObject ret = new JSObject();
            ret.put("count", count);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute count", ex);
        }
    }

    private MongoCursor<Document> _find(PluginCall call) {
        Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
        if (filterDoc == null) {
            filterDoc = new Document();
        }

        MongoDatabase db = getDatabase(call);
        MongoCollection<Document> collection = getCollection(call, db);

        FindIterable<Document> fi = collection.find(filterDoc);
        fi = OptionParser.applyFindOptions(fi, call.getObject("options"));

        MongoCursor<Document> cursor = fi.iterator();

        return cursor;
    }

    private MongoCursor<Document> _execAggregate(PluginCall call) throws InvalidParameterException {
        List<Document> pipeline = null;
        try {
            pipeline = OptionParser.getDocumentArray(call.getArray("pipeline"));
        } catch (Exception ex) {}
        if (pipeline == null) {
            throw new InvalidParameterException("pipeline must be provided and must be an array of pipeline operations");
        }

//        let aggOpts = try OptionsParser.getAggregateOptions(call.getObject("options"))
        MongoDatabase db = getDatabase(call);
        MongoCollection<Document> collection = getCollection(call, db);
        AggregateIterable<Document> ai = collection.aggregate(pipeline);
        ai = OptionParser.applyAggregateOptions(ai, call.getObject("options"));

        MongoCursor<Document> cursor = ai.iterator();

        return cursor;
    }

    @PluginMethod()
    public void find(PluginCall call) {
        try {
            boolean useCursor = call.getBoolean("cursor", false);

            MongoCursor<Document> cursor = _find(call);

            if (useCursor) {
                returnCursor(call, cursor);
            } else {
                returnDocsFromCursor(call, cursor);
            }
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute find", ex);
        }
    }

    @PluginMethod()
    public void aggregate(PluginCall call) {
        try {
            boolean useCursor = call.getBoolean("cursor", false);

            MongoCursor<Document> cursor = _execAggregate(call);

            if (useCursor) {
                returnCursor(call, cursor);
            } else {
                returnDocsFromCursor(call, cursor);
            }
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute aggregate", ex);
        }
    }

    @PluginMethod()
    public void cursorGetNext(PluginCall call) {
        try {
            String cursorIdStr = call.getString("cursorId", "n/a");
            UUID cursorId = null;
            try {
                cursorId = UUID.fromString(cursorIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("cursorId must be provided and must be a string");
            }

            MongoCursor<Document> cursor = cursorMap.remove(cursorId);

            if (cursor == null) {
                throw new InvalidParameterException("cursorId does not refer to a valid cursor");
            }

            boolean useBson = call.getBoolean("useBson");
            int batchSize = call.getInt("batchSize", 1);
            if (batchSize < 1) {
                throw new InvalidParameterException("batchSize must be at least 1");
            }

            JSArray resultsJson = new JSArray();
            while (cursor.hasNext()) {
                // TODO: Handle BSON output!
                Document cur = cursor.next();
                resultsJson.put(new JSONObject(cur.toJson(jsonSettings)));
                if (resultsJson.length() >= batchSize) {
                    break;
                }
            }

            JSObject ret = new JSObject();
            ret.put("results", resultsJson);
            if (resultsJson.length() == 0) {
                // This is the end! Close the cursor and mark it complete
                ret.put("complete", true);
                cursor.close();
                cursorMap.remove(cursorId);
            }

            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute closeCursor", ex);
        }

    }

    @PluginMethod()
    public void closeCursor(PluginCall call) {
        try {
            String cursorIdStr = call.getString("cursorId", "n/a");
            UUID cursorId = null;
            try {
                cursorId = UUID.fromString(cursorIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("cursorId must be provided and must be a string");
            }

            MongoCursor<Document> cursor = cursorMap.remove(cursorId);

            JSObject ret = new JSObject();
            ret.put("success", true);
            if (cursor != null) {
                cursor.close();
                ret.put("removed", true);
            } else {
                ret.put("removed", true);
            }
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute closeCursor", ex);
        }
    }


    /*******************
     ** WRITE METHODS **
     *******************/
    @PluginMethod()
    public void insertOne(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);
            Document doc = null;
            try {
                doc = OptionParser.getDocument(call.getObject("doc"));
            } catch (Exception ex) {}
            if (doc == null) {
                throw new InvalidParameterException("doc must be a valid document object");
            }

            InsertOneOptions opts = OptionParser.getInsertOneOptions(call.getObject("options"));

            collection.insertOne(doc, opts);

            JSObject ret = new JSObject();

            ret.put("success", true);

            if (doc.containsKey("_id")) {
                try {
                    JSONObject fullDoc = new JSONObject(doc.toJson(jsonSettings));
                    ret.put("insertedId", fullDoc.get("_id"));
                } catch (JSONException ex) {
                    ret.put("insertedId", null);
                }
            } else {
                ret.put("insertedId", null);
            }
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }

    }
    @PluginMethod()
    public void insertMany(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            JSArray jsArr = call.getArray("docs");
            if (jsArr == null) {
                throw new InvalidParameterException("docs must be a valid array of documents to insert");
            }
            List<Document> docs = OptionParser.getDocumentArray(jsArr);

            InsertManyOptions opts = OptionParser.getInsertManyOptions(call.getObject("options"));

            collection.insertMany(docs, opts);

            JSObject ret = new JSObject();
            if (collection.getWriteConcern().getW() == 0) {
                // Write preference is "don't wait", so we don't
                // know how the write went
                ret.put("success", true);
                ret.put("insertedCount", null);
                ret.put("insertedIds", null);
                call.resolve(ret);
                return;
            }

            // If we waited then we need to return a summary of what we inserted
            JSArray insertedIds = new JSArray();
            int insertedCount = 0;
            for (Document doc : docs) {
                try {
                    JSObject docJson = new JSObject(doc.toJson(jsonSettings));
                    insertedIds.put(docJson.get("_id"));
                } catch (JSONException ex) {
                    insertedIds.put(null);
                }
                insertedCount++;
            }

            ret.put("success", true);
            ret.put("insertedCount", insertedCount);
            ret.put("insertedIds", insertedIds);
            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }
    private void returnUpdateResult(PluginCall call, UpdateResult res) {
        JSObject ret = new JSObject();
        ret.put("success", true);
        if (res.wasAcknowledged()) {
            ret.put("matchedCount", res.getMatchedCount());
            ret.put("modifiedCount", res.isModifiedCountAvailable() ? res.getModifiedCount() : null);
            BsonValue upsertedId = res.getUpsertedId();
            ret.put("upsertedCount", upsertedId != null ? 1 : 0);
            if (upsertedId != null) {
                ret.put("upsertedCount", 1);
                ret.put("upsertedId", OptionParser.bsonToJson(upsertedId));
            } else {
                ret.put("upsertedCount", 0);
                ret.put("upsertedId", null);
            }
        } else {
            ret.put("matchedCount", null);
            ret.put("modifiedCount", null);
            ret.put("upsertedCount", null);
            ret.put("upsertedId", null);
        }

        call.resolve(ret);

    }
    @PluginMethod()
    public void replaceOne(PluginCall call) {
        try {
            Document doc = null;
            try {
                doc = OptionParser.getDocument(call.getObject("doc"));
            } catch (Exception ex) {}
            if (doc == null) {
                throw new InvalidParameterException("doc must be a valid document object");
            }
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }

            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            ReplaceOptions opts = OptionParser.getReplaceOptions(call.getObject("options"));

            UpdateResult result = collection.replaceOne(filterDoc, doc, opts);

            returnUpdateResult(call, result);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }
    @PluginMethod()
    public void updateOne(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            Document update = null;
            try {
                update = OptionParser.getDocument(call.getObject("update"));
            } catch (Exception ex) {}
            if (update == null) {
                throw new InvalidParameterException("update must be a valid document object");
            }
            UpdateOptions opts = OptionParser.getUpdateOptions(call.getObject("options"));

            UpdateResult result = collection.updateOne(filterDoc, update, opts);

            returnUpdateResult(call, result);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }
    @PluginMethod()
    public void updateMany(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            Document update = null;
            try {
                update = OptionParser.getDocument(call.getObject("update"));
            } catch (Exception ex) {}
            if (update == null) {
                throw new InvalidParameterException("update must be a valid document object");
            }
            UpdateOptions opts = OptionParser.getUpdateOptions(call.getObject("options"));

            UpdateResult result = collection.updateMany(filterDoc, update, opts);

            returnUpdateResult(call, result);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }
    @PluginMethod()
    public void deleteOne(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            DeleteOptions opts = OptionParser.getDeleteOptions(call.getObject("options"));

            DeleteResult result = collection.deleteOne(filterDoc, opts);

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("deletedCount", result.wasAcknowledged() ? result.getDeletedCount() : null);

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }
    @PluginMethod()
    public void deleteMany(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            DeleteOptions opts = OptionParser.getDeleteOptions(call.getObject("options"));

            DeleteResult result = collection.deleteMany(filterDoc, opts);

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("deletedCount", result.wasAcknowledged() ? result.getDeletedCount() : null);

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropCollection", ex);
        }
    }


    /*******************
     ** INDEX METHODS **
     *******************/
    @PluginMethod()
    public void createIndexes(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            JSArray indexes = call.getArray("indexes");
            if (indexes == null || indexes.length() == 0) {
                throw new InvalidParameterException("indexes must be Array<[keys: Document, options?: IndexOptions]> with length >= 1");
            }
            ArrayList<IndexModel> indexModels = new ArrayList<>();

            for (int i = 0; i < indexes.length(); ++i) {
                JSONArray curDef;
                try {
                    curDef = indexes.getJSONArray(i);
                } catch (JSONException ex) {
                    throw new InvalidParameterException("indexes[" + i + "] must be [keys: Document, options?: IndexOptions] (as a 1 or 2 element array)");
                }
                JSONObject keys = curDef.getJSONObject(0);
                JSONObject opts = curDef.length() > 1 ? curDef.getJSONObject(1) : null;

                indexModels.add(OptionParser.getIndexModel(keys, opts));
            }

            List<String> createResults = collection.createIndexes(indexModels);

            JSObject ret = new JSObject();
            ret.put("indexesCreated", new JSArray(createResults));

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute createIndexes", ex);
        }
    }

    @PluginMethod()
    public void dropIndex(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            String name = call.getString("name");
            JSObject keys = call.getObject("keys");

            DropIndexOptions opts = new DropIndexOptions();
            // TODO: maxtime is supported only on android; should we support it?

            if (name != null) {
                collection.dropIndex(name, opts);
            } else if (keys != null) {
                Document keysDoc = OptionParser.getDocument(keys);
                collection.dropIndex(keysDoc);
            } else {
                throw new InvalidParameterException("name: string or keys: {[keyName: string]: 1|-1} expected");
            }

            JSObject ret = new JSObject();
            ret.put("done", true);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute dropIndex", ex);
        }
    }

    @PluginMethod()
    public void listIndexes(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            MongoCursor<Document> cursor = collection.listIndexes().iterator();
            returnDocsFromCursor(call, cursor);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute listIndexes", ex);
        }
    }
}
