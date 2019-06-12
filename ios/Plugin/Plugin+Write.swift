//
//  Plugin+Write.swift
//  Plugin
//
//  Created by Richard Bateman on 6/11/19.
//  Copyright © 2019 Max Lynch. All rights reserved.
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
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let doc = try OptionsParser.getDocument(call.getObject("doc"), "doc") else {
                throw MDBAPIError.invalidArguments(message: "doc must be provided and must be a Document object")
            }
            let insertOneOpts = try OptionsParser.getInsertOneOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let insertResult = try collection.insertOne(doc, options: insertOneOpts)
            if (insertResult == nil) {
                call.resolve([
                    "success": true,
                    "insertedId": nil
                    ])
                return
            }
            
            call.resolve([
                "success": true,
                "insertedId": bsonToJsValue(insertResult!.insertedId) as Any
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
        
    }
    @objc func insertMany(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let docs = try OptionsParser.getDocumentArray(call.getArray("doc", Any.self), "docs") else {
                throw MDBAPIError.invalidArguments(message: "docs must be provided and must be an arrat of Document objects")
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
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            guard let doc = try OptionsParser.getDocument(call.getObject("doc"), "doc") else {
                throw MDBAPIError.invalidArguments(message: "doc must be provided and must be a Document object")
            }
            let replaceOneOpts = try OptionsParser.getReplaceOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let replaceResult = try collection.replaceOne(filter: query, replacement: doc, options: replaceOneOpts)
            if (replaceResult == nil) {
                let res: PluginResultData = [
                    "success": true,
                    "matchedCount": -1,
                    "modifiedCount": -1,
                    "upsertedId": "null",
                    "upsertedCount": -1
                ]
                call.resolve(res)
                return
            }
            
            call.resolve([
                "success": true,
                "matchedCount": replaceResult!.matchedCount,
                "modifiedCount": replaceResult!.modifiedCount,
                "upsertedId": replaceResult!.upsertedId != nil ? bsonToJsValue(replaceResult!.upsertedId!.value) : "null",
                "upsertedCount": replaceResult!.upsertedCount
            ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func updateOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw MDBAPIError.invalidArguments(message: "doc must be provided and must be a Document object")
            }
            let updateOneOpts = try OptionsParser.getUpdateOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let updateResult = try collection.updateOne(filter: query, update: update, options: updateOneOpts)
            if (updateResult == nil) {
                let res: PluginResultData = [
                    "success": true,
                    "matchedCount": -1,
                    "modifiedCount": -1,
                    "upsertedId": "null",
                    "upsertedCount": -1
                ]
                call.resolve(res)
                return
            }
            
            call.resolve([
                "success": true,
                "matchedCount": updateResult!.matchedCount,
                "modifiedCount": updateResult!.modifiedCount,
                "upsertedId": updateResult!.upsertedId != nil ? bsonToJsValue(updateResult!.upsertedId!.value) : "null",
                "upsertedCount": updateResult!.upsertedCount
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func updateMany(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw MDBAPIError.invalidArguments(message: "doc must be provided and must be a Document object")
            }
            let updateOpts = try OptionsParser.getUpdateOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let updateResult = try collection.updateMany(filter: query, update: update, options: updateOpts)
            if (updateResult == nil) {
                let res: PluginResultData = [
                    "success": true,
                    "matchedCount": -1,
                    "modifiedCount": -1,
                    "upsertedId": "null",
                    "upsertedCount": -1
                ]
                call.resolve(res)
                return
            }
            
            call.resolve([
                "success": true,
                "matchedCount": updateResult!.matchedCount,
                "modifiedCount": updateResult!.modifiedCount,
                "upsertedId": updateResult!.upsertedId != nil ? bsonToJsValue(updateResult!.upsertedId!.value) : "null",
                "upsertedCount": updateResult!.upsertedCount
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    @objc func deleteOne(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            let deleteOpts = try OptionsParser.getDeleteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let deleteResult = try collection.deleteOne(query, options: deleteOpts)
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
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            let deleteOpts = try OptionsParser.getDeleteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let deleteResult = try collection.deleteMany(query, options: deleteOpts)
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
