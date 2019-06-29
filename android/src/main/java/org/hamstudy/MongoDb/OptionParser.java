package org.hamstudy.MongoDb;

import android.util.Base64;

import com.getcapacitor.JSObject;
import com.mongodb.CursorType;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptionDefaults;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;



public class OptionParser {
    private static JsonWriterSettings jsonSettings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    static String InvalidArgErrorPrefix = "Invalid type for variable ";
    public static boolean getBool(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            return obj.getBoolean(name);
        } catch (Exception ex) {}
        try {
            String strObj = obj.getString(name);
            return strObj.equals("true");
        } catch (Exception ex) {}
        try {
            Integer intObj = obj.getInt(name);
            return intObj != 0;
        } catch (Exception ex) {}

        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected boolean (true/false)");
    }
    public static int getInt32(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            return obj.getInt(name);
        } catch (Exception ex) {}

        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected integer");
    }
    public static long getInt64(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            return obj.getLong(name);
        } catch (Exception ex) {}
        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected integer");
    }
    public static double getDouble(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            return obj.getDouble(name);
        } catch (Exception ex) {}

        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected double");
    }
    public static String getString(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            return obj.getString(name);
        } catch (Exception ex) {}
        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected string");
    }
    public static Document getDocument(JSONObject obj) {
        if (obj.has("$b64")) {
            // This is a BSON document base64 encoded! Decode it.
            try {
                byte[] rawBytes = Base64.decode(obj.getString("$b64"), Base64.DEFAULT);
                RawBsonDocument bsDoc = new RawBsonDocument(rawBytes);

                // TODO: Is there a more efficient way to convert from a RawBsonDocument to a Document?
                // Seems like there should be, but I can't find it.
                return Document.parse(bsDoc.toJson(jsonSettings));
            } catch (JSONException ex) {}
        }

        return Document.parse(obj.toString());
    }
    public static Document getDocument(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            JSONObject jsdoc = obj.getJSONObject(name);
            return getDocument(jsdoc);
        } catch (Exception ex) {}

        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; Could not parse document");
    }
    static CursorType getCursorType(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }

        try {
            String strObj = getString(obj, name);
            switch (strObj) {
                case "tailable":
                    return CursorType.Tailable;
                case "nonTailable":
                    return CursorType.NonTailable;
                case "tailableAwait":
                    return CursorType.TailableAwait;
            }
        } catch (Exception ex) {
        }
        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected 'tailable' | 'nonTailable' | 'tailableAwait'");
    }
    public static JSObject bsonToJson(BsonValue inVal) {
        BsonDocument doc = new BsonDocument();
        doc.put("_id", inVal);
        try {
            JSObject jsDoc = new JSObject(doc.toJson(jsonSettings));
            return jsDoc.getJSObject("_id");
        } catch (JSONException ex) {
            return null;
        }
    }
    public static ArrayList<Document> getDocumentArray(JSONArray arr) throws InvalidParameterException {
        ArrayList<Document> outList = new ArrayList<Document>();
        for (int i = 0; i < arr.length(); ++i) {
            try {
                outList.add(getDocument(arr.getJSONObject(i)));
            } catch (JSONException ex) {
                throw new InvalidParameterException(String.valueOf(i));
            }
        }
        return outList;
    }
    public static ArrayList<Document> getDocumentArray(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }

        try {
            JSONArray arr = obj.getJSONArray(name);
            return getDocumentArray(arr);
        } catch (JSONException ex) {
            throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected array of documents");
        } catch (InvalidParameterException ex) {
            throw new InvalidParameterException(InvalidArgErrorPrefix + name + "[" + ex.toString() + "]; expected Document");
        }
    }
    public static WriteConcern getWriteConcern(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }

        try {
            JSONObject wcObj = obj.getJSONObject(name);
            int w = getInt32(wcObj, "w");
            WriteConcern wc = new WriteConcern(w);
            try {
                wc = wc.withJournal(getBool(wcObj, "j"));
            } catch (Exception ex) {}
            try {
                wc = wc.withWTimeout(getInt32(wcObj, "wtimeout"), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {}
            return wc;
        } catch (JSONException ex) {};

        try {
            int w = getInt32(obj, name);
            return new WriteConcern(w);
        } catch (Exception ex) {}

        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected number or {w?: number, j?: boolean, wtimeout?: number}");
    }
    public static Collation getCollation(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        JSONObject collationSrc;
        try {
            collationSrc = obj.getJSONObject(name);
        } catch (JSONException ex) {
            throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; if provided must be a valid collation document");
        }
        Collation.Builder builder = Collation.builder();

        try {
            builder.locale(getString(collationSrc, "locale"));
        } catch (InvalidKeyException ex) {}
        try {
            builder.caseLevel(getBool(collationSrc, "caseLevel"));
        } catch (InvalidKeyException ex) {}
        try {
            builder.collationCaseFirst(CollationCaseFirst.fromString(getString(collationSrc, "caseFirst")));
        } catch (InvalidKeyException ex) {}
        try {
            builder.collationStrength(CollationStrength.fromInt(getInt32(collationSrc, "strength")));
        } catch (InvalidKeyException ex) {}
        try {
            builder.numericOrdering(getBool(collationSrc, "numericOrdering"));
        } catch (InvalidKeyException ex) {}
        try {
            builder.collationAlternate(CollationAlternate.fromString(getString(collationSrc, "alternate")));
        } catch (InvalidKeyException ex) {}
        try {
            builder.collationMaxVariable(CollationMaxVariable.fromString(getString(collationSrc, "maxVariable")));
        } catch (InvalidKeyException ex) {}
        try {
            builder.backwards(getBool(collationSrc, "backwards"));
        } catch (InvalidKeyException ex) {}
        return builder.build();
    }
    public static CreateCollectionOptions getCreateCollectionOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        CreateCollectionOptions opts = new CreateCollectionOptions();

        try {
            opts.capped(getBool(obj, "capped"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.sizeInBytes(getInt64(obj, "size"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.maxDocuments(getInt64(obj, "max"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.storageEngineOptions(getDocument(obj, "storageEngine"));
        } catch (InvalidKeyException ex) {}

        ValidationOptions vOpts = new ValidationOptions();
        try {
            vOpts.validator(getDocument(obj, "validator"));
        } catch (InvalidKeyException ex) {}
        try {
            vOpts.validationAction(ValidationAction.fromString(getString(obj, "validationAction")));
        } catch (InvalidKeyException ex) {}
        try {
            vOpts.validationLevel(ValidationLevel.fromString(getString(obj, "validationLevel")));
        } catch (InvalidKeyException ex) {}
        opts.validationOptions(vOpts);

        try {
            IndexOptionDefaults iOpts = new IndexOptionDefaults();
            iOpts.storageEngine(getDocument(obj, "indexOptionDefaults"));
            opts.indexOptionDefaults(iOpts);
        } catch (InvalidKeyException ex) {}

        try {
            Collation collation = getCollation(obj, "collation");
            opts.collation(collation);
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static CountOptions getCountOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        CountOptions opts = new CountOptions();
        try {
            Collation collation = getCollation(obj, "collation");
            opts.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            opts.hint(getDocument(obj, "hint"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.limit(getInt32(obj, "limit"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            opts.skip(getInt32(obj, "skip"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }
    public static InsertOneOptions getInsertOneOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        InsertOneOptions opts = new InsertOneOptions();
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        return opts;
    }
    public static InsertManyOptions getInsertManyOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        InsertManyOptions opts = new InsertManyOptions();
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.ordered(getBool(obj, "ordered"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static UpdateOptions getUpdateOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        UpdateOptions opts = new UpdateOptions();
        try {
            opts.arrayFilters(getDocumentArray(obj, "arrayFilters"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            Collation collation = getCollation(obj, "collation");
            opts.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            opts.upsert(getBool(obj, "upsert"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static ReplaceOptions getReplaceOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        ReplaceOptions opts = new ReplaceOptions();

        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            Collation collation = getCollation(obj, "collation");
            opts.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            opts.upsert(getBool(obj, "upsert"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static DeleteOptions getDeleteOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        DeleteOptions opts = new DeleteOptions();
        try {
            Collation collation = getCollation(obj, "collation");
            opts.collation(collation);
        } catch (InvalidKeyException ex) {}

        return opts;
    }
    public static <TDocument> FindIterable<TDocument> applyFindOptions(FindIterable<TDocument> it, JSONObject obj) {
        if (obj == null) {
            return it;
        }

        try {
            it.partial(getBool(obj, "allowPartialResults"));
        } catch (InvalidKeyException ex) {}
        try {
            it.batchSize(getInt32(obj, "batchSize"));
        } catch (InvalidKeyException ex) {}
        try {
            Collation collation = getCollation(obj, "collation");
            it.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            it.comment(getString(obj, "comment"));
        } catch (InvalidKeyException ex) {}
        try {
            CursorType cType = getCursorType(obj, "cursorType");
            it.cursorType(cType);
        } catch (InvalidKeyException ex) {}
        try {
            it.hint(getDocument(obj, "hint"));
        } catch (InvalidKeyException ex) {}
        try {
            it.limit(getInt32(obj, "limit"));
        } catch (InvalidKeyException ex) {}
        try {
            it.max(getDocument(obj, "max"));
        } catch (InvalidKeyException ex) {}
        try {
            it.maxAwaitTime(getInt32(obj, "maxAwaitTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            it.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            it.min(getDocument(obj, "min"));
        } catch (InvalidKeyException ex) {}
        try {
            it.noCursorTimeout(getBool(obj, "noCursorTimeout"));
        } catch (InvalidKeyException ex) {}
        try {
            it.projection(getDocument(obj, "projection"));
        } catch (InvalidKeyException ex) {}
        try {
            it.returnKey(getBool(obj, "returnKey"));
        } catch (InvalidKeyException ex) {}
        try {
            it.showRecordId(getBool(obj, "showRecordId"));
        } catch (InvalidKeyException ex) {}
        try {
            it.skip(getInt32(obj, "skip"));
        } catch (InvalidKeyException ex) {}
        try {
            it.sort(getDocument(obj, "sort"));
        } catch (InvalidKeyException ex) {}

        return it;
    }
    public static <TDocument> AggregateIterable<TDocument> applyAggregateOptions(AggregateIterable<TDocument> it, JSONObject obj) {
        if (obj == null) {
            return it;
        }

        try {
            it.allowDiskUse(getBool(obj, "allowDiskUse"));
        } catch (InvalidKeyException ex) {}
        try {
            it.batchSize(getInt32(obj, "batchSize"));
        } catch (InvalidKeyException ex) {}
        try {
            it.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            Collation collation = getCollation(obj, "collation");
            it.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            it.comment(getString(obj, "comment"));
        } catch (InvalidKeyException ex) {}
        try {
            it.hint(getDocument(obj, "hint"));
        } catch (InvalidKeyException ex) {}
        try {
            it.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}

        return it;
    }
    public static IndexOptions getIndexOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        IndexOptions opts = new IndexOptions();
        try {
            opts.background(getBool(obj, "background"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.expireAfter(getInt64(obj, "expireAfter"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            opts.name(getString(obj, "name"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.sparse(getBool(obj, "sparse"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.storageEngine(new BsonString(getString(obj, "storageEngine")).asDocument());
        } catch (InvalidKeyException ex) {}
        try {
            opts.unique(getBool(obj, "unique"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.version(getInt32(obj, "version"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.defaultLanguage(getString(obj, "defaultLanguage"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.languageOverride(getString(obj, "languageOverride"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.textVersion(getInt32(obj, "textVersion"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.weights(getDocument(obj, "weights"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.sphereVersion(getInt32(obj, "sphereVersion"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.bits(getInt32(obj, "bits"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.max(getDouble(obj, "max"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.min(getDouble(obj, "min"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.bucketSize(getDouble(obj, "bucketSize"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.partialFilterExpression(getDocument(obj, "partialFilterExpression"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.collation(getCollation(obj, "collation"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }
    public static IndexModel getIndexModel(JSONObject keysObj, JSONObject optsObj) {
        if (keysObj == null) {
            return null;
        }
        IndexOptions opts = optsObj != null ? getIndexOptions(optsObj) : null;

        Document keys = getDocument(keysObj);

        IndexModel idx = new IndexModel(keys, opts);

        return idx;
    }

    public static FindOneAndDeleteOptions getFindOneAndDeleteOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        FindOneAndDeleteOptions opts = new FindOneAndDeleteOptions();
        try {
            opts.collation(getCollation(obj, "collation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            opts.projection(getDocument(obj, "projection"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.sort(getDocument(obj, "sort"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static FindOneAndReplaceOptions getFindOneAndReplaceOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        FindOneAndReplaceOptions opts = new FindOneAndReplaceOptions();
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.collation(getCollation(obj, "collation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            opts.projection(getDocument(obj, "projection"));
        } catch (InvalidKeyException ex) {}
        try {
            boolean returnNewDocument = getBool(obj, "returnNewDocument");
            opts.returnDocument(returnNewDocument ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
        } catch (InvalidKeyException ex) {}
        try {
            opts.sort(getDocument(obj, "sort"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.upsert(getBool(obj, "upsert"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static FindOneAndUpdateOptions getFindOneAndUpdateOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions();
        try {
            opts.arrayFilters(getDocumentArray(obj, "arrayFilters"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.collation(getCollation(obj, "collation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            opts.projection(getDocument(obj, "projection"));
        } catch (InvalidKeyException ex) {}
        try {
            boolean returnNewDocument = getBool(obj, "returnNewDocument");
            opts.returnDocument(returnNewDocument ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
        } catch (InvalidKeyException ex) {}
        try {
            opts.sort(getDocument(obj, "sort"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.upsert(getBool(obj, "upsert"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }

    public static BulkWriteOptions getBulkWriteOptions(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        BulkWriteOptions opts = new BulkWriteOptions();
        try {
            opts.bypassDocumentValidation(getBool(obj, "bypassDocumentValidation"));
        } catch (InvalidKeyException ex) {}
        try {
            opts.ordered(getBool(obj, "ordered"));
        } catch (InvalidKeyException ex) {}

        return opts;
    }
}
