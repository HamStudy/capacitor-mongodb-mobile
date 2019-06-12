//
//  OptionParser.swift
//  Plugin
//
//  Created by Richard Bateman on 6/11/19.
//  Copyright © 2019 Max Lynch. All rights reserved.
//

import Foundation
import Capacitor
import MongoSwift

let InvalidArgErrorPrefix = "Invalid type for variable "

public class OptionsParser {
    static func getBool(_ obj: Any?, _ name: String) throws -> Bool? {
        if obj == nil {
            return nil
        } else if let boolObj = obj as? Bool {
            return boolObj;
        } else if let strObj = obj as? String {
            return strObj == "true"
        } else if let numObj = obj as? Int {
            return numObj != 0
        } else {
            throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected boolean (true/false)")
        }
    }
    static func getInt32(_ obj: Any?, _ name: String) throws -> Int32? {
        if obj == nil {
            return nil
        } else if let intObj = obj as? Int32 {
            return intObj
        } else {
            throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected integer");
        }
    }
    static func getInt64(_ obj: Any?, _ name: String) throws -> Int64? {
        if obj == nil {
            return nil
        } else if let intObj = obj as? Int64 {
            return intObj
        } else {
            throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected integer");
        }
    }
    static func getString(_ obj: Any?, _ name: String) throws -> String? {
        if obj == nil {
            return nil
        } else if let strObj = obj as? String {
            return strObj
        } else {
            throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected string");
        }
    }
    static func getDocument(_ obj: Any?, _ name: String) throws -> Document? {
        if obj == nil {
            return nil
        }
        do {
            return try Document(fromJSON: JSONSerialization.data(withJSONObject: obj!, options: .prettyPrinted))
        } catch RuntimeError.internalError(let message) {
            throw UserError.invalidArgumentError(message: "Runtime error processing \(name): \(message)")
        }
    }
    static func getCursorType(_ obj: Any?, _ name: String) throws -> CursorType? {
        let strObj = try getString(obj, name)
        
        if strObj == nil {
            return nil
        }
        
        if strObj == "tailable" {
            return CursorType.tailable
        } else if strObj == "nonTailable" {
            return CursorType.nonTailable
        } else if strObj == "tailableAwait" {
            return CursorType.tailableAwait
        } else {
            throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected 'tailable' | 'nonTailable' | 'tailableAwait'")
        }
    }
    static func getHint(_ obj: Any?, _ name: String) throws -> Hint? {
        if obj == nil {
            return nil
        }
        
        if let strObj = obj as? String {
            return Hint.indexName(strObj)
        } else {
            do {
                return Hint.indexSpec(try getDocument(obj, name)!)
            } catch UserError.invalidArgumentError {
                throw UserError.invalidArgumentError(message: InvalidArgErrorPrefix + "\(name); expected index name or spec document")
            }
        }
    }

    static func getFindOptions(_ obj: [String : Any]?) throws -> FindOptions? {
        if obj == nil {
            return nil
        }
        let allowPartialResults = try getBool(obj!["allowPartialResults"], "allowPartialResults");
        let batchSize = try getInt32(obj!["batchSize"], "batchSize");
        let collation = try getDocument(obj!["collation"], "collation");
        let comment = try getString(obj!["comment"], "comment");
        let cursorType = try getCursorType(obj!["cursorType"], "cursorType")
        let hint: Hint? = try getHint(obj!["hint"], "hint");
        let limit: Int64? = try getInt64(obj!["limit"], "limit")
        let max: Document? = try getDocument(obj!["max"], "max")
        let maxAwaitTimeMS: Int64? = try getInt64(obj!["maxAwaitTimeMS"], "maxAwaitTimeMS")
        let maxScan: Int64? = try getInt64(obj!["maxScan"], "maxScan")
        let maxTimeMS: Int64? = try getInt64(obj!["maxTimeMS"], "maxTimeMS")
        let min: Document? = try getDocument(obj!["min"], "min")
        let noCursorTimeout: Bool? = try getBool(obj!["noCursorTimeout"], "noCursorTimeout")
        let projection: Document? = try getDocument(obj!["projection"], "projection")
        let readConcern: ReadConcern? = nil // This is never needed for a local db
        let readPreference: ReadPreference? = nil // This is never needed for a local db
        let returnKey: Bool? = try getBool(obj!["returnKey"], "returnKey")
        let showRecordId: Bool? = try getBool(obj!["showRecordId"], "showRecordId")
        let skip: Int64? = try getInt64(obj!["skip"], "skip")
        let sort: Document? = try getDocument(obj!["document"], "document")
        
        return FindOptions(allowPartialResults: allowPartialResults,
                           batchSize: batchSize,
                           collation: collation,
                           comment: comment,
                           cursorType: cursorType,
                           hint: hint,
                           limit: limit,
                           max: max,
                           maxAwaitTimeMS: maxAwaitTimeMS,
                           maxScan: maxScan,
                           maxTimeMS: maxTimeMS,
                           min: min,
                           noCursorTimeout: noCursorTimeout,
                           projection: projection,
                           readConcern: readConcern,
                           readPreference: readPreference,
                           returnKey: returnKey,
                           showRecordId: showRecordId,
                           skip: skip,
                           sort: sort)
    }
    
    static func getCountOptions(_ obj: [String : Any]?) throws -> CountOptions? {
        if obj == nil {
            return nil
        }
        let collation = try getDocument(obj!["collation"], "collation");
        let hint: Hint? = try getHint(obj!["hint"], "hint");
        let limit: Int64? = try getInt64(obj!["limit"], "limit")
        let maxTimeMS: Int64? = try getInt64(obj!["maxTimeMS"], "maxTimeMS")
        let readConcern: ReadConcern? = nil // This is never needed for a local db
        let readPreference: ReadPreference? = nil // This is never needed for a local db
        let skip: Int64? = try getInt64(obj!["skip"], "skip")
        
        return CountOptions(collation: collation,
                            hint: hint,
                            limit: limit,
                            maxTimeMS: maxTimeMS,
                            readConcern: readConcern,
                            readPreference: readPreference,
                            skip: skip
        )
    }

    static func getAggregateOptions(_ obj: [String: Any]?) throws -> AggregateOptions? {
        if obj == nil {
            return nil
        }
        
        let allowDiskUse = try getBool(obj!["allowDiskUse"], "allowDiskUse")
        let batchSize = try getInt32(obj!["batchSize"], "batchSize")
        let bypassDocumentValidation = try getBool(obj!["bypassDocumentValidation"], "bypassDocumentValidation")
        let collation = try getDocument(obj!["collation"], "collation")
        let comment = try getString(obj!["comment"], "comment")
        let hint = try getHint(obj!["hint"], "hint")
        let maxTimeMS = try getInt64(obj!["maxTimeMS"], "maxTimeMS")
        let readConcern: ReadConcern? = nil
        let readPreference: ReadPreference? = nil
        let writeConcern: WriteConcern? = nil
        
        return AggregateOptions(allowDiskUse: allowDiskUse,
                                batchSize: batchSize,
                                bypassDocumentValidation: bypassDocumentValidation,
                                collation: collation,
                                comment: comment,
                                hint: hint,
                                maxTimeMS: maxTimeMS,
                                readConcern: readConcern,
                                readPreference: readPreference,
                                writeConcern: writeConcern)
    }
}
