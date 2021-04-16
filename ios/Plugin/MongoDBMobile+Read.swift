//
//  Plugin+Read.swift
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
    @objc func count(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            guard let filter = call.getObject("filter") else {
                throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
            }
            let countOpts = try OptionsParser.getCountOptions(call.getObject("options"))
            let filterDoc = try OptionsParser.getDocument(filter, "filter")
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let count = try collection.count(filterDoc!, options: countOpts)
            
            call.resolve([
                "count": count
                ])
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute count")
        }
        
    }
    
    private func _find(_ call: CAPPluginCall) throws -> MongoCursor<Document> {
        guard let dbName = call.getString("db") else {
            throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
        }
        guard let collectionName = call.getString("collection") else {
            throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
        }
        guard let filter = call.getObject("filter") else {
            throw UserError.invalidArgumentError(message: "filter must be provided and must be an object")
        }
        let findOpts = try OptionsParser.getFindOptions(call.getObject("options"))
        let filterDoc = try OptionsParser.getDocument(filter, "filter")
        let db = mongoClient!.db(dbName)
        let collection = db.collection(collectionName)
        let cursor = try collection.find(filterDoc!, options: findOpts)
        
        return cursor
    }
    
    private func _execAggregate(_ call: CAPPluginCall) throws -> MongoCursor<Document> {
        guard let dbName = call.getString("db") else {
            throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
        }
        guard let collectionName = call.getString("collection") else {
            throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
        }
        guard let pipeline = try OptionsParser.getDocumentArray(call.getArray("pipeline", Any.self), "pipeline") else {
            throw UserError.invalidArgumentError(message: "pipeline must be provided and must be an array of pipeline operations")
        }
        let aggOpts = try OptionsParser.getAggregateOptions(call.getObject("options"))
        let db = mongoClient!.db(dbName)
        let collection = db.collection(collectionName)
        let cursor = try collection.aggregate(pipeline, options: aggOpts)
        
        return cursor
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
        } catch UserError.invalidArgumentError(let message) {
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
                throw UserError.invalidArgumentError(message: "cursorId must be provided and must be a string")
            }
            guard let cursorUuid = UUID(uuidString: cursorIdStr) else {
                throw UserError.invalidArgumentError(message: "cursorId is not in a valid format")
            }
            guard let cursor = cursorMap[cursorUuid] else {
                throw UserError.invalidArgumentError(message: "cursorId does not refer to a valid cursor")
            }
            // if useBson is true we will return base64 encoded raw bson
            let useBson = call.getBool("useBson", false)!
            
            let batchSize = call.getInt("batchSize") ?? 1
            if batchSize < 1 {
                throw UserError.invalidArgumentError(message: "batchSize must be at least 1")
            }
            var resultsJson: [Any] = []
            if useBson {
                for doc in cursor {
                    resultsJson.append([
                        "$b64": doc.rawBSON.base64EncodedString()
                    ])
                    if resultsJson.count >= batchSize {
                        break
                    }
                }
            } else {
                for doc in cursor {
                    resultsJson.append(convertToDictionary(text: doc.canonicalExtendedJSON)!)
                    if resultsJson.count >= batchSize {
                        break
                    }
                }
            }
            if (resultsJson.count == 0) {
                call.resolve([
                    "results": resultsJson,
                    "complete": true
                    ])
                cursor.close()
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
                throw UserError.invalidArgumentError(message: "cursorId must be provided and must be a string")
            }
            guard let cursorUuid = UUID(uuidString: cursorIdStr) else {
                throw UserError.invalidArgumentError(message: "cursorId is not in a valid format")
            }
            
            let cursor = cursorMap.removeValue(forKey: cursorUuid)
            if cursor != nil {
                cursor!.close()
                call.resolve(["success": true, "removed": true])
            } else {
                call.resolve(["success": true, "removed": false])
            }
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not close cursor")
        }
    }
    
    
}
