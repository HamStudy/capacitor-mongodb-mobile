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
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.DeleteManyModel;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@NativePlugin()
public class MongoDBMobile extends Plugin {

    private JsonWriterSettings jsonSettings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    // Create a Client for MongoDB Mobile (initializing MongoDB Mobile)
    MongoClient mongoClient;

    HashMap<UUID, MongoCursor<Document>> cursorMap = new HashMap<>();
    HashMap<UUID, MongoCursor<RawBsonDocument>> cursorMapBson = new HashMap<>();

    HashMap<UUID, BulkWriteBatch<Document>> bulkMap = new HashMap<>();

    @PluginMethod()
    public void initDb(PluginCall call) {
        String appId = call.getString("appId", getAppId());
        final StitchAppClient client =
                Stitch.initializeDefaultAppClient(appId);

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
    <TDocument> MongoCollection<TDocument> getCollection(PluginCall call, MongoDatabase db, Class<TDocument> documentClass) {
        String collectionName = call.getString("collection", "");
        if (collectionName.isEmpty()) {
            throw new InvalidParameterException("collection name must be provided and must be a string");
        }
        MongoCollection collection = db.getCollection(collectionName, documentClass);
        return collection;
    }
    private MongoCollection<Document> getCollection(PluginCall call, MongoDatabase db) {
        return getCollection(call, db, Document.class);
    }

    /**
     * Helper for handling errors
     */
    private void handleError(PluginCall call, String message) {
        handleError(call, message, null);
    }
    private void handleError(PluginCall call, String message, Exception error) {
        call.reject(message, error);
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

    private JSObject getBsonBase64Doc(RawBsonDocument doc) {
        ByteBuf data = doc.getByteBuffer();
        String b64String = Base64.encodeToString(data.array(), Base64.DEFAULT);
        JSObject obj = new JSObject();
        obj.put("$b64", b64String);
        return obj;
    }

    /**
     * Helper to return a bson cursor to the page
     * @param call
     * @param cursor
     */
    private void returnCursorBson(PluginCall call, MongoCursor<RawBsonDocument> cursor) {

        UUID cursorId = UUID.randomUUID();

        cursorMapBson.put(cursorId, cursor);

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
            resultsJson.put(getBsonBase64Doc(cur));
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
            handleError(call, "Could not execute dropDatabase: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute listCollections: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute createCollection: " + ex.getMessage(), ex);
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

            ArrayList<String> names = db.listCollectionNames().into(new ArrayList<String>());
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
            handleError(call, "Could not execute dropCollection: " + ex.getMessage(), ex);
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
            Document command = OptionParser.getDocument(commandSrc);

            boolean useBson = call.getBoolean("useBson");

            JSObject ret = new JSObject();
            if (useBson) {
                RawBsonDocument reply = db.runCommand(command, RawBsonDocument.class);
                ret.put("reply", getBsonBase64Doc(reply));
            } else {
                Document reply = db.runCommand(command);

                ret.put("reply", new JSONObject(reply.toJson(jsonSettings)));
            }
            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute runCommand: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute count: " + ex.getMessage(), ex);
        }
    }

    private MongoCursor<Document> _find(PluginCall call) {
        return _find(call, Document.class);
    }
    private <TDocument> MongoCursor<TDocument> _find(PluginCall call, Class<TDocument> documentClass) {
        Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
        if (filterDoc == null) {
            filterDoc = new Document();
        }

        MongoDatabase db = getDatabase(call);
        MongoCollection<TDocument> collection = getCollection(call, db, documentClass);

        FindIterable<TDocument> fi = collection.find(filterDoc);
        fi = OptionParser.applyFindOptions(fi, call.getObject("options"));

        MongoCursor<TDocument> cursor = fi.iterator();

        return cursor;
    }

    private MongoCursor<Document> _execAggregate(PluginCall call) throws InvalidParameterException {
        return _execAggregate(call, Document.class);
    }
    private <TDocument> MongoCursor<TDocument> _execAggregate(PluginCall call, Class<TDocument> documentClass) throws InvalidParameterException {
        List<Document> pipeline = null;
        try {
            pipeline = OptionParser.getDocumentArray(call.getArray("pipeline"));
        } catch (Exception ex) {}
        if (pipeline == null) {
            throw new InvalidParameterException("pipeline must be provided and must be an array of pipeline operations");
        }

        MongoDatabase db = getDatabase(call);
        MongoCollection<TDocument> collection = getCollection(call, db, documentClass);
        AggregateIterable<TDocument> ai = collection.aggregate(pipeline);
        ai = OptionParser.applyAggregateOptions(ai, call.getObject("options"));

        MongoCursor<TDocument> cursor = ai.iterator();

        return cursor;
    }

    @PluginMethod()
    public void find(PluginCall call) {
        try {
            boolean useCursor = call.getBoolean("cursor", false);
            boolean useBson = call.getBoolean("useBson", false);

            if (useBson) {
                MongoCursor<RawBsonDocument> cursor = _find(call, RawBsonDocument.class);
                if (useCursor) {
                    returnCursorBson(call, cursor);
                } else {
                    returnDocsFromCursorBson(call, cursor);
                }
            } else {
                MongoCursor<Document> cursor = _find(call);
                if (useCursor) {
                    returnCursor(call, cursor);
                } else {
                    returnDocsFromCursor(call, cursor);
                }
            }

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute find: " + ex.getMessage(), ex);
        }
    }

    @PluginMethod()
    public void aggregate(PluginCall call) {
        try {
            boolean useCursor = call.getBoolean("cursor", false);
            boolean useBson = call.getBoolean("useBson", false);

            if (useBson) {
                MongoCursor<RawBsonDocument> cursor = _execAggregate(call, RawBsonDocument.class);
                if (useCursor) {
                    returnCursorBson(call, cursor);
                } else {
                    returnDocsFromCursorBson(call, cursor);
                }
            } else {
                MongoCursor<Document> cursor = _execAggregate(call);
                if (useCursor) {
                    returnCursor(call, cursor);
                } else {
                    returnDocsFromCursor(call, cursor);
                }
            }
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute aggregate: " + ex.getMessage(), ex);
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

            MongoCursor<Document> cursor = cursorMap.get(cursorId);
            MongoCursor<RawBsonDocument> bsonCursor = cursorMapBson.get(cursorId);

            if (cursor == null && bsonCursor == null) {
                throw new InvalidParameterException("cursorId does not refer to a valid cursor");
            }

            int batchSize = call.getInt("batchSize", 1);
            if (batchSize < 1) {
                throw new InvalidParameterException("batchSize must be at least 1");
            }

            JSObject ret = new JSObject();
            JSArray resultsJson = new JSArray();
            if (cursor != null) {
                // normal output
                while (cursor.hasNext()) {
                    Document cur = cursor.next();
                    resultsJson.put(new JSONObject(cur.toJson(jsonSettings)));
                    if (resultsJson.length() >= batchSize) {
                        break;
                    }
                }

                ret.put("results", resultsJson);
                if (resultsJson.length() == 0) {
                    // This is the end! Close the cursor and mark it complete
                    ret.put("complete", true);
                    cursor.close();
                    cursorMap.remove(cursorId);
                }
            } else {
                // BSON base64 output
                while (bsonCursor.hasNext()) {
                    RawBsonDocument cur = bsonCursor.next();
                    resultsJson.put(getBsonBase64Doc(cur));
                    if (resultsJson.length() >= batchSize) {
                        break;
                    }
                }

                ret.put("results", resultsJson);
                if (resultsJson.length() == 0) {
                    // This is the end! Close the cursor and mark it complete
                    ret.put("complete", true);
                    bsonCursor.close();
                    cursorMapBson.remove(cursorId);
                }
            }

            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute cursorGetNext: " + ex.getMessage(), ex);
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
            MongoCursor<RawBsonDocument> bsonCursor = cursorMapBson.remove(cursorId);

            JSObject ret = new JSObject();
            ret.put("success", true);
            if (cursor != null) {
                cursor.close();
                ret.put("removed", true);
            } else if (bsonCursor != null) {
                bsonCursor.close();
                ret.put("removed", true);
            } else {
                ret.put("removed", true);
            }
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute closeCursor: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute insertOne: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute insertMany: " + ex.getMessage(), ex);
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
            Document replacement = null;
            try {
                replacement = OptionParser.getDocument(call.getObject("replacement"));
            } catch (Exception ex) {}
            if (replacement == null) {
                throw new InvalidParameterException("replacement must be a valid document object");
            }
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }

            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            ReplaceOptions opts = OptionParser.getReplaceOptions(call.getObject("options"));

            UpdateResult result = collection.replaceOne(filterDoc, replacement, opts);

            returnUpdateResult(call, result);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute replaceOne: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute updateOne: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute updateMany: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute deleteOne: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute deleteMany: " + ex.getMessage(), ex);
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
            String[] resultsArr = createResults.toArray(new String[createResults.size()]);
            ret.put("indexesCreated", new JSArray(resultsArr));

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute createIndexes: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute dropIndex: " + ex.getMessage(), ex);
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
            handleError(call, "Could not execute listIndexes: " + ex.getMessage(), ex);
        }
    }


    /***************************
     ** FINDANDMODIFY METHODS **
     ***************************/
    @PluginMethod()
    public void findOneAndDelete(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            FindOneAndDeleteOptions opts = OptionParser.getFindOneAndDeleteOptions(call.getObject("options"));
            boolean useBson = call.getBoolean("useBson", false);


            JSObject ret = new JSObject();
            if (useBson) {
                MongoCollection<RawBsonDocument> collection = getCollection(call, db, RawBsonDocument.class);

                RawBsonDocument doc = collection.findOneAndDelete(filterDoc, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", getBsonBase64Doc(doc));
                }
            } else {
                MongoCollection<Document> collection = getCollection(call, db);

                Document doc = collection.findOneAndDelete(filterDoc, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", new JSObject(doc.toJson(jsonSettings)));
                }
            }

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute findOneAndDelete: " + ex.getMessage(), ex);
        }
    }

    @PluginMethod()
    public void findOneAndReplace(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            Document replacement = null;
            try {
                replacement = OptionParser.getDocument(call.getObject("replacement"));
            } catch (Exception ex) {}
            if (replacement == null) {
                throw new InvalidParameterException("replacement must be a valid document object");
            }
            FindOneAndReplaceOptions opts = OptionParser.getFindOneAndReplaceOptions(call.getObject("options"));
            boolean useBson = call.getBoolean("useBson", false);


            JSObject ret = new JSObject();
            if (useBson) {
                MongoCollection<RawBsonDocument> collection = getCollection(call, db, RawBsonDocument.class);
                RawBsonDocument repl = RawBsonDocument.parse(replacement.toJson(jsonSettings));

                RawBsonDocument doc = collection.findOneAndReplace(filterDoc, repl, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", getBsonBase64Doc(doc));
                }
            } else {
                MongoCollection<Document> collection = getCollection(call, db);

                Document doc = collection.findOneAndReplace(filterDoc, replacement, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", new JSObject(doc.toJson(jsonSettings)));
                }
            }

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute findOneAndReplace: " + ex.getMessage(), ex);
        }
    }

    @PluginMethod()
    public void findOneAndUpdate(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
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
            FindOneAndUpdateOptions opts = OptionParser.getFindOneAndUpdateOptions(call.getObject("options"));
            boolean useBson = call.getBoolean("useBson", false);

            JSObject ret = new JSObject();
            if (useBson) {
                MongoCollection<RawBsonDocument> collection = getCollection(call, db, RawBsonDocument.class);

                RawBsonDocument doc = collection.findOneAndUpdate(filterDoc, update, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", getBsonBase64Doc(doc));
                }
            } else {
                MongoCollection<Document> collection = getCollection(call, db);

                Document doc = collection.findOneAndUpdate(filterDoc, update, opts);
                if (doc == null) {
                    ret.put("doc", null);
                } else {
                    ret.put("doc", new JSObject(doc.toJson(jsonSettings)));
                }
            }

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute findOneAndUpdate: " + ex.getMessage(), ex);
        }
    }


    /****************************
     ** BULK OPERATION METHODS **
     ****************************/
    @PluginMethod()
    public void newBulkWrite(PluginCall call) {
        try {
            MongoDatabase db = getDatabase(call);
            MongoCollection<Document> collection = getCollection(call, db);

            BulkWriteOptions opts = OptionParser.getBulkWriteOptions(call.getObject("options"));

            UUID opId = UUID.randomUUID();

            BulkWriteBatch<Document> batch = new BulkWriteBatch<>(collection, opts);
            bulkMap.put(opId, batch);

            JSObject ret = new JSObject();
            ret.put("operationId", opId.toString());
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute newBulkWrite: " + ex.getMessage(), ex);
        }
    }
    @PluginMethod()
    public void bulkWriteAddDeleteOne(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }

            DeleteOptions opts = OptionParser.getDeleteOptions(call.getObject("options"));

            DeleteOneModel<Document> operation = new DeleteOneModel<>(filterDoc, opts);

            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddDeleteOne: " + ex.getMessage(), ex);
        }

    }
    @PluginMethod()
    public void bulkWriteAddDeleteMany(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }

            DeleteOptions opts = OptionParser.getDeleteOptions(call.getObject("options"));
            DeleteManyModel<Document> operation = new DeleteManyModel<>(filterDoc, opts);
            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddDeleteMany: " + ex.getMessage(), ex);
        }

    }
    @PluginMethod()
    public void bulkWriteAddInsertOne(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

            Document doc = null;
            try {
                doc = OptionParser.getDocument(call.getObject("doc"));
            } catch (Exception ex) {}
            if (doc == null) {
                throw new InvalidParameterException("doc must be a valid document object");
            }

            InsertOneModel<Document> operation = new InsertOneModel<>(doc);
            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddInsertOne: " + ex.getMessage(), ex);
        }

    }
    @PluginMethod()
    public void bulkWriteAddReplaceOne(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

            Document filterDoc = OptionParser.getDocument(call.getObject("filter"));
            if (filterDoc == null) {
                filterDoc = new Document();
            }
            Document replacement = null;
            try {
                replacement = OptionParser.getDocument(call.getObject("replacement"));
            } catch (Exception ex) {}
            if (replacement == null) {
                throw new InvalidParameterException("replacement must be a valid document object");
            }

            ReplaceOptions opts = OptionParser.getReplaceOptions(call.getObject("options"));
            ReplaceOneModel<Document> operation = new ReplaceOneModel<>(filterDoc, replacement, opts);
            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddReplaceOne: " + ex.getMessage(), ex);
        }

    }
    @PluginMethod()
    public void bulkWriteAddUpdateOne(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

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

            UpdateOptions opts = OptionParser.getUpdateOptions(call.getObject("update"));
            UpdateOneModel<Document> operation = new UpdateOneModel<>(filterDoc, update, opts);
            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddUpdateOne: " + ex.getMessage(), ex);
        }

    }

    @PluginMethod()
    public void bulkWriteAddUpdateMany(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

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

            UpdateOptions opts = OptionParser.getUpdateOptions(call.getObject("update"));
            UpdateManyModel<Document> operation = new UpdateManyModel<>(filterDoc, update, opts);
            batch.addRequest(operation);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteAddUpdateMany: " + ex.getMessage(), ex);
        }

    }

    @PluginMethod()
    public void bulkWriteCancel(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.remove(operationId);

            JSObject ret = new JSObject();

            ret.put("removed", batch != null);

            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteCancel: " + ex.getMessage(), ex);
        }
    }

    @PluginMethod()
    public void bulkWriteExecute(PluginCall call) {
        try {
            String opIdStr = call.getString("operationId", "n/a");
            UUID operationId = null;
            try {
                operationId = UUID.fromString(opIdStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterException("operationId must be provided and must be a string");
            }

            BulkWriteBatch<Document> batch = bulkMap.get(operationId);

            if (batch == null) {
                throw new InvalidParameterException("operationId does not refer to a valid bulk operation");
            }

            BulkWriteResult result = batch.execute();

            JSObject ret = new JSObject();

            if (!result.wasAcknowledged()) {
                ret.put("deletedCount", null);
                ret.put("insertedCount", null);
                ret.put("matchedCount", null);
                ret.put("modifiedCount", null);
                ret.put("upsertedCount", null);
                ret.put("insertedIds", null);
                ret.put("upsertedIds", null);
            } else {
                ret.put("deletedCount", result.getDeletedCount());
                ret.put("insertedCount", result.getInsertedCount());
                ret.put("matchedCount", result.getMatchedCount());
                ret.put("modifiedCount", result.getModifiedCount());

                List<BulkWriteUpsert> upsertList = result.getUpserts();
                ret.put("upsertedCount", upsertList.size());
                JSArray upsertedIds = new JSArray();
                for (BulkWriteUpsert upsert : upsertList) {
                    upsertedIds.put(OptionParser.bsonToJson(upsert.getId()));
                }
                ret.put("upsertedIds", upsertedIds);
                ret.put("insertedIds", null);
            }

            bulkMap.remove(operationId);
            call.resolve(ret);

        } catch (InvalidParameterException ex) {
            handleError(call, ex.getMessage(), ex);
        } catch (Exception ex) {
            handleError(call, "Could not execute bulkWriteExecute: " + ex.getMessage(), ex);
        }

    }

}
