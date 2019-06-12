//
//  Plugin+Read.swift
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
    func returnCursor(_ call: CAPPluginCall, cursor: MongoCursor<Document>) {
        let cursorId = UUID()
        
        cursorMap[cursorId] = cursor
        
        call.resolve([
            "cursorId": cursorId.uuidString
            ])
    }
    func returnDocsFromCursor(_ call: CAPPluginCall, cursor: MongoCursor<Document>) {
        var resultsJson: [Any] = []
        for doc in cursor {
            resultsJson.append(convertToDictionary(text: doc.extendedJSON)!)
        }
        call.resolve(["results": resultsJson])
    }
    
    @objc func listDatabases(_ call: CAPPluginCall) {
        do {
            let dbListCursor = try mongoClient!.listDatabases()
            
            var resultsJson: [Any] = []
            for doc in dbListCursor {
                resultsJson.append(convertToDictionary(text: doc.extendedJSON)!)
            }
            call.resolve(["databases": resultsJson])
        } catch {
            handleError(call, "Could not list databases!")
        }
    }
    
    @objc func count(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
            }
            guard let query = call.getObject("query") else {
                throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
            }
            let countOpts = try OptionsParser.getCountOptions(call.getObject("options"))
            let queryDoc = try OptionsParser.getDocument(query, "query")
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let count = try collection.count(queryDoc!, options: countOpts)
            
            call.resolve([
                "count": count
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
        
    }
    
    private func _find(_ call: CAPPluginCall) throws -> MongoCursor<Document> {
        guard let dbName = call.getString("db") else {
            throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
        }
        guard let collectionName = call.getString("collection") else {
            throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
        }
        guard let query = call.getObject("query") else {
            throw MDBAPIError.invalidArguments(message: "query must be provided and must be an object")
        }
        let findOpts = try OptionsParser.getFindOptions(call.getObject("options"))
        let queryDoc = try OptionsParser.getDocument(query, "query")
        let db = mongoClient!.db(dbName)
        let collection = db.collection(collectionName)
        let cursor = try collection.find(queryDoc!, options: findOpts)
        
        return cursor
    }
    
    private func _execAggregate(_ call: CAPPluginCall) throws -> MongoCursor<Document> {
        guard let dbName = call.getString("db") else {
            throw MDBAPIError.invalidArguments(message: "db name must be provided and must be a string")
        }
        guard let collectionName = call.getString("collection") else {
            throw MDBAPIError.invalidArguments(message: "collection name must be provided and must be a string")
        }
        guard let pipeline = try OptionsParser.getDocumentArray(call.getArray("pipeline", Any.self), "pipeline") else {
            throw MDBAPIError.invalidArguments(message: "pipeline must be provided and must be an array of pipeline operations")
        }
        let aggOpts = try OptionsParser.getAggregateOptions(call.getObject("options"))
        let db = mongoClient!.db(dbName)
        let collection = db.collection(collectionName)
        let resultsDocuments = try collection.aggregate(pipeline, options: aggOpts)
        
        return resultsDocuments
    }
    
    @objc func find(_ call: CAPPluginCall) {
        do {
            let useCursor = call.getBool("cursor", false)!
            
            let cursor = try _find(call)
            
            if useCursor {
                returnCursor(call, cursor: cursor)
            } else {
                returnDocsFromCursor(call, cursor: cursor)
            }
        } catch MDBAPIError.invalidArguments(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute find")
        }
    }
    
    @objc func aggregate(_ call: CAPPluginCall) {
        do {
            let useCursor = call.getBool("cursor", false)!
            
            let cursor = try _execAggregate(call)
            
            if useCursor {
                returnCursor(call, cursor: cursor)
            } else {
                returnDocsFromCursor(call, cursor: cursor)
            }
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute aggregate")
        }
    }
    
    @objc func cursorGetNext(_ call: CAPPluginCall) {
        do {
            guard let cursorIdStr = call.getString("cursorId") else {
                throw MDBAPIError.invalidArguments(message: "cusrorId must be provided and must be a string")
            }
            guard let cursorUuid = UUID(uuidString: cursorIdStr) else {
                throw MDBAPIError.invalidArguments(message: "cusrorId is not in a valid format")
            }
            guard let cursor = cursorMap[cursorUuid] else {
                throw MDBAPIError.invalidArguments(message: "cusrorId does not refer to a valid cursor")
            }
            
            let batchSize = call.getInt("batchSize") ?? 1
            if batchSize < 1 {
                throw MDBAPIError.invalidArguments(message: "batchSize must be at least 1")
            }
            var resultsJson: [Any] = []
            for doc in cursor {
                resultsJson.append(convertToDictionary(text: doc.extendedJSON)!)
                if resultsJson.count >= batchSize {
                    break
                }
            }
            if (resultsJson.count == 0) {
                call.resolve([
                    "results": resultsJson,
                    "complete": true
                    ])
                cursorMap.removeValue(forKey: cursorUuid)
            } else {
                call.resolve(["results": resultsJson])
            }
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not get next from cursor")
        }
    }
    
    @objc func closeCursor(_ call: CAPPluginCall) {
        do {
            guard let cursorIdStr = call.getString("cursorId") else {
                throw MDBAPIError.invalidArguments(message: "cusrorId must be provided and must be a string")
            }
            guard let cursorUuid = UUID(uuidString: cursorIdStr) else {
                throw MDBAPIError.invalidArguments(message: "cusrorId is not in a valid format")
            }
            
            if cursorMap.removeValue(forKey: cursorUuid) != nil {
                call.resolve(["success": true, "removed": false])
            } else {
                call.resolve(["success": true, "removed": true])
            }
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not close cursor")
        }
    }
    
    
}
