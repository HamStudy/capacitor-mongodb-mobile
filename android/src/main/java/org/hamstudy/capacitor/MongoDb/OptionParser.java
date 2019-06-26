package org.hamstudy.capacitor.MongoDb;

import com.getcapacitor.JSObject;
import com.mongodb.CursorType;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptionDefaults;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.util.concurrent.TimeUnit;



public class OptionParser {
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
        return Document.parse(obj.toString());
    }
    public static Document getDocument(JSONObject obj, String name) throws InvalidParameterException, InvalidKeyException {
        if (!obj.has(name)) {
            throw new InvalidKeyException(name);
        }
        try {
            JSONObject jsdoc = obj.getJSONObject(name);
            // TODO: Support bson documents (base64 encoded)

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
        } catch (Exception ex) {}
        throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected 'tailable' | 'nonTailable' | 'tailableAwait'");
    }
//    static func getHint(JSObject obj, String name) throws InvalidParameterException, InvalidKeyException {
//        if obj == nil {
//            return nil
//        }
//
//        if let strObj = obj as? String {
//            return Hint.indexName(strObj)
//        } else {
//            do {
//                return Hint.indexSpec(try getDocument(obj, name)!)
//            } catch UserError.invalidArgumentError {
//                throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected index name or spec document")
//            }
//        }
//    }
//    static func getDocumentArray(JSObject obj, String name) throws InvalidParameterException, InvalidKeyException {
//        if obj == nil {
//            return nil
//        }
//
//        if let arrObj = obj as? [Any] {
//            var arr: [Document] = []
//            for (i, step) in arrObj.enumerated() {
//                guard let doc = try getDocument(step, "pipeline[\(i)]") else {
//                    throw UserError.invalidArgumentError(message: "Invalid type for variable \(name)[\(i)]: expected document")
//                }
//                arr.append(doc)
//            }
//            return arr
//        } else {
//            throw new InvalidParameterException(InvalidArgErrorPrefix + name + "; expected array of documents")
//        }
//    }
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
    public static FindIterable<Document> applyFindOptions(FindIterable<Document> fi, JSONObject obj) {

        try {
            fi.partial(getBool(obj, "allowPartialResults"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.batchSize(getInt32(obj, "batchSize"));
        } catch (InvalidKeyException ex) {}
        try {
            Collation collation = getCollation(obj, "collation");
            fi.collation(collation);
        } catch (InvalidKeyException ex) {}
        try {
            fi.comment(getString(obj, "comment"));
        } catch (InvalidKeyException ex) {}
        try {
            CursorType cType = getCursorType(obj, "cursorType");
            fi.cursorType(cType);
        } catch (InvalidKeyException ex) {}
        try {
            fi.hint(getDocument(obj, "hint"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.limit(getInt32(obj, "limit"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.max(getDocument(obj, "max"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.maxAwaitTime(getInt32(obj, "maxAwaitTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            fi.maxTime(getInt64(obj, "maxTimeMS"), TimeUnit.MILLISECONDS);
        } catch (InvalidKeyException ex) {}
        try {
            fi.min(getDocument(obj, "min"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.noCursorTimeout(getBool(obj, "noCursorTimeout"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.projection(getDocument(obj, "projection"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.returnKey(getBool(obj, "returnKey"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.showRecordId(getBool(obj, "showRecordId"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.skip(getInt32(obj, "skip"));
        } catch (InvalidKeyException ex) {}
        try {
            fi.sort(getDocument(obj, "sort"));
        } catch (InvalidKeyException ex) {}

        return fi;
    }
}
