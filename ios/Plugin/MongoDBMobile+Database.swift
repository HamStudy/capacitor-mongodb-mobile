//
//  MongoDBMobile+Database.swift
//  Plugin
//
//  Created by Richard Bateman on 6/13/19.
//

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
    @objc func listDatabases(_ call: CAPPluginCall) {
        do {
            let dbListCursor = try mongoClient!.listDatabases()
            
            var resultsJson: [Any] = []
            for doc in dbListCursor {
                resultsJson.append(convertToDictionary(text: doc.canonicalExtendedJSON)!)
            }
            call.resolve(["databases": resultsJson])
        } catch {
            handleError(call, "Could not list databases!")
        }
    }
    @objc func dropDatabase(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            let db = mongoClient!.db(dbName)
            try db.drop()
            
            let data: PluginResultData = [
                "dropped": true
            ]
            call.resolve(data)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute dropDatabase")
        }
    }
    

    @objc func listCollections(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            
            let db = mongoClient!.db(dbName)
            let collections = try db.listCollections()

            var resultsJson: [Any] = []
            for c in collections {
                resultsJson.append(convertToDictionary(text: c.canonicalExtendedJSON)!)
            }
            
            let data: PluginResultData = [
                "collections": resultsJson
            ]
            call.resolve(data)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute listCollections")
        }
    }
    
    @objc func createCollection(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            let db = mongoClient!.db(dbName)
            
            let opts = try OptionsParser.getCreateCollectionOptions(call.getObject("options"))
            _ = try db.createCollection(collectionName, options: opts)
            
            let data: PluginResultData = [
                "collection": collectionName
            ]
            call.resolve(data)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute createCollection")
        }
    }
    @objc func dropCollection(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            try collection.drop()
            
            let data: PluginResultData = [
                "dropped": true
            ]
            call.resolve(data)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute dropCollection")
        }
    }
    
    @objc func runCommand(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let command = try OptionsParser.getDocument(call.getObject("command"), "command") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            let opts = try OptionsParser.getRunCommandOptions(call.getObject("options"))

            let db = mongoClient!.db(dbName)
            
            let reply = try db.runCommand(command, options: opts)
            
            let data: PluginResultData = [
                "reply": convertToDictionary(text: reply.canonicalExtendedJSON)!
            ]
            
            call.resolve(data)
            
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute runCommand")
        }
    }
    
    
}
