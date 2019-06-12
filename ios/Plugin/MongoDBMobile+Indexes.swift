//
//  MongoDBMobile+Indexes.swift
//  Plugin
//
//  Created by Richard Bateman on 6/12/19.
//  Copyright © 2019 Max Lynch. All rights reserved.
//

import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

extension MongoDBMobile {
    @objc func createIndexes(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let indexes = call.getArray("indexes", Array<[String: Any?]>.self) else {
                throw UserError.invalidArgumentError(message: "indexes must be Arrray<[keys: Document, options?: IndexOptions]>")
            }
            var indexModels: [IndexModel] = []
            for (i, index) in indexes.enumerated() {
                let indexOpts = index.count > 0 ? index[1] : nil
                
                let curModel = try OptionsParser.getIndexModel(modelObj: index[0], modelField: "indexes[\(i)][0]", optsObj: indexOpts)
                indexModels.append(curModel)
            }
            
            let createOptions = try OptionsParser.getCreateIndexOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let createResults = try collection.createIndexes(indexModels, options: createOptions)
            
            call.resolve([
                "indexesCreated": createResults
            ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not create indexes")
        }
    }
    
    @objc func dropIndex(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            let name = call.getString("name")
            let keys = call.getString("keys")
            
            let dropOptions = try OptionsParser.getDropIndexOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            if name != nil {
                try collection.dropIndex(name!, options: dropOptions)
            } else if keys != nil {
                let keysDoc = try OptionsParser.getDocument(keys, "keys")
                try collection.dropIndex(keysDoc!)
            } else {
                throw UserError.invalidArgumentError(message: "name: string or keys: {[keyName: string]: 1|-1} expected")
            }
            
            call.resolve([
                "done": true
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not drop index")
        }
    }
    
    @objc func listIndexes(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)

            let cursor = try collection.listIndexes()
            returnDocsFromCursor(call, cursor: cursor)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not drop index")
        }
    }
}
