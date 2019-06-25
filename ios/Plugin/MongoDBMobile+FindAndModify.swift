//
//  MongoDBMobile+FindAndModify.swift
//  Plugin
//
//  Created by Richard Bateman on 6/12/19.
//

import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

extension MongoDBMobile {
    
    @objc func findOneAndDelete(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw UserError.invalidArgumentError(message: "query must be provided and must be a Document object")
            }
            let opts = try OptionsParser.getFindOneAndDeleteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let doc = try collection.findOneAndDelete(filter, options: opts)
            var outData: PluginResultData = [:]
            if (doc != nil) {
                outData["doc"] = convertToDictionary(text: doc!.canonicalExtendedJSON)!
            } else {
                outData["doc"] = nil
            }
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    
    @objc func findOneAndReplace(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw UserError.invalidArgumentError(message: "query must be provided and must be a Document object")
            }
            guard let replacement = try OptionsParser.getDocument(call.getObject("replacement"), "replacement") else {
                throw UserError.invalidArgumentError(message: "replacement must be provided and must be a Document object")
            }
            let opts = try OptionsParser.getFindOneAndReplaceOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let doc = try collection.findOneAndReplace(filter: filter, replacement: replacement, options: opts)
            var outData: PluginResultData = [:]
            if (doc != nil) {
                outData["doc"] = convertToDictionary(text: doc!.canonicalExtendedJSON)!
            } else {
                outData["doc"] = nil
            }
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    
    @objc func findOneAndUpdate(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = try OptionsParser.getDocument(call.getObject("query"), "query") else {
                throw UserError.invalidArgumentError(message: "query must be provided and must be a Document object")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw UserError.invalidArgumentError(message: "update must be provided and must be a Document object")
            }
            let opts = try OptionsParser.getFindOneAndUpdateOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let doc = try collection.findOneAndUpdate(filter: filter, update: update, options: opts)
            var outData: PluginResultData = [:]
            if (doc != nil) {
                outData["doc"] = convertToDictionary(text: doc!.canonicalExtendedJSON)!
            } else {
                outData["doc"] = nil
            }
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    
}

