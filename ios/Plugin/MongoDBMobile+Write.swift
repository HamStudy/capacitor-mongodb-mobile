//
//  Plugin+Write.swift
//  Plugin
//
//  Created by Richard Bateman on 6/11/19.
//

import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

extension MongoDBMobile {
    func bsonToJsValue(_ bsonVal: BSONValue) -> Any? {
        switch bsonVal.bsonType {
        case .array:
            let arrVal = bsonVal as! Array<BSONValue>
            return arrVal.map { bsonToJsValue($0) }
        case .null:
            return nil
        case .binary:
            return [
                "base64":(bsonVal as! Binary).data.base64EncodedString()
            ]
        case .boolean, .dateTime, .double, .int32, .int64, .string:
            return bsonVal
        case .objectId:
            return (bsonVal as! ObjectId).hex
        case .regularExpression:
            return [
                "$regex": (bsonVal as! RegularExpression).pattern,
                "$options": (bsonVal as! RegularExpression).options
            ]
        default:
            return bsonVal
        }
    }
    
    @objc func insertOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let doc = try OptionsParser.getDocument(call.getObject("doc"), "doc") else {
                throw UserError.invalidArgumentError(message: "doc must be provided and must be a Document object")
            }
            let insertOneOpts = try OptionsParser.getInsertOneOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let insertResult = try collection.insertOne(doc, options: insertOneOpts)
            var data: PluginResultData = [:]
            data["success"] = true
            data["insertedId"] = nil
            if (insertResult != nil) {
                data["insertedId"] = bsonToJsValue(insertResult!.insertedId)
            }
            
            call.resolve(data)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
        
    }
    @objc func insertMany(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let docs = try OptionsParser.getDocumentArray(call.getArray("doc", Any.self), "docs") else {
                throw UserError.invalidArgumentError(message: "docs must be provided and must be an arrat of Document objects")
            }
            let insertManyOpts = try OptionsParser.getInsertManyOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let insertResult = try collection.insertMany(docs, options: insertManyOpts)
            if (insertResult == nil) {
                call.resolve([
                    "success": true,
                    "insertedCount": -1,
                    "insertedIds": []
                    ])
                return
            }
            
            call.resolve([
                "success": true,
                "insertedCount": insertResult!.insertedCount,
                "insertedIds": insertResult!.insertedIds.mapValues {bsonToJsValue($0)}
            ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func replaceOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            guard let doc = try OptionsParser.getDocument(call.getObject("doc"), "doc") else {
                throw UserError.invalidArgumentError(message: "doc must be provided and must be a Document object")
            }
            let replaceOneOpts = try OptionsParser.getReplaceOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let replaceResult = try collection.replaceOne(filter: filter, replacement: doc, options: replaceOneOpts)
            var res: PluginResultData = [
                "success": true,
                "matchedCount": -1,
                "modifiedCount": -1,
                "upsertedCount": -1
            ]
            res["upsertedId"] = nil
            
            if (replaceResult != nil) {
                res["matchedCount"] = replaceResult!.matchedCount
                res["modifiedCount"] = replaceResult!.modifiedCount
                res["upsertedCount"] = replaceResult!.upsertedCount
                if (replaceResult!.upsertedId != nil) {
                    res["upsertedId"] = bsonToJsValue(replaceResult!.upsertedId!.value)
                }
                call.resolve(res)
                return
            }
            
            call.resolve(res)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func updateOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw UserError.invalidArgumentError(message: "doc must be provided and must be a Document object")
            }
            let updateOneOpts = try OptionsParser.getUpdateOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let updateResult = try collection.updateOne(filter: filter, update: update, options: updateOneOpts)
            var res: PluginResultData = [
                "success": true,
                "matchedCount": -1,
                "modifiedCount": -1,
                "upsertedCount": -1
            ]
            res["upsertedId"] = nil
            if (updateResult != nil) {
                res["matchedCount"] = updateResult!.matchedCount
                res["modifiedCount"] = updateResult!.modifiedCount
                res["upsertedCount"] = updateResult!.upsertedCount
                if (updateResult!.upsertedId != nil) {
                    res["upsertedId"] = bsonToJsValue(updateResult!.upsertedId!.value)
                }
            }

            call.resolve(res)

        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func updateMany(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw UserError.invalidArgumentError(message: "doc must be provided and must be a Document object")
            }
            let updateOpts = try OptionsParser.getUpdateOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let updateResult = try collection.updateMany(filter: filter, update: update, options: updateOpts)
            var res: PluginResultData = [
                "success": true,
                "matchedCount": -1,
                "modifiedCount": -1,
                "upsertedCount": -1
            ]
            res["upsertedId"] = nil
            if (updateResult != nil) {
                res["matchedCount"] = updateResult!.matchedCount
                res["modifiedCount"] = updateResult!.modifiedCount
                res["upsertedCount"] = updateResult!.upsertedCount
                if (updateResult!.upsertedId != nil) {
                    res["upsertedId"] = bsonToJsValue(updateResult!.upsertedId!.value)
                }
            }
            call.resolve(res)
            
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func deleteOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            let deleteOpts = try OptionsParser.getDeleteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let deleteResult = try collection.deleteOne(filter, options: deleteOpts)
            if (deleteResult == nil) {
                let res: PluginResultData = [
                    "success": true,
                    "deletedCount": -1,
                ]
                call.resolve(res)
                return
            }
            
            call.resolve([
                "success": true,
                "deletedCount": deleteResult!.deletedCount,
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func deleteMany(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            let deleteOpts = try OptionsParser.getDeleteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let deleteResult = try collection.deleteMany(filter, options: deleteOpts)
            if (deleteResult == nil) {
                let res: PluginResultData = [
                    "success": true,
                    "deletedCount": -1,
                ]
                call.resolve(res)
                return
            }
            
            call.resolve([
                "success": true,
                "deletedCount": deleteResult!.deletedCount,
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
}
