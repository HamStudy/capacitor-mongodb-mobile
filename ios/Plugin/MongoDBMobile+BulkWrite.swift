//
//  MongoDBMobile+BulkWrite.swift
//  Plugin
//
//  Created by Richard Bateman on 6/12/19.
//

import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

struct BulkWriteBatch {
    private let collection: MongoCollection<Document>;
    private let opts: BulkWriteOptions?;
    private var requests: [WriteModel] = [];
    
    mutating func addRequest(_ req: WriteModel) {
        requests.append(req)
    }
    
    public func execute() throws -> BulkWriteResult? {
        return try collection.bulkWrite(self.requests, options: self.opts)
    }
    
    public init(collection: MongoCollection<Document>,
                options: BulkWriteOptions?) {
        self.collection = collection
        self.opts = options
    }
}

extension MongoDBMobile {
    
    @objc func newBulkWrite(_ call: CAPPluginCall) {
        do {
            guard let dbName = call.getString("db") else {
                throw UserError.invalidArgumentError(message: "db name must be provided and must be a string")
            }
            guard let collectionName = call.getString("collection") else {
                throw UserError.invalidArgumentError(message: "collection name must be provided and must be a string")
            }
            
            let opts = try OptionsParser.getBulkWriteOptions(call.getObject("options"))
            
            let db = mongoClient!.db(dbName)
            let collection = db.collection(collectionName)
            
            let opId = UUID()
            
            var outData: PluginResultData = [:]
            
            self.bulkOperations[opId] = BulkWriteBatch(collection: collection, options: opts)
            
            outData["operationId"] = opId.uuidString
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    @objc func bulkWriteAddDeleteOne(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be a valid document")
            }
            guard let deleteModelOpt = try OptionsParser.getDeleteModelOption(call.getObject("options")) else {
                throw UserError.invalidArgumentError(message: "options (if provided) must contain a valid collation document")
            }
            
            let writeModel = MongoCollection<Document>.DeleteOneModel(filter, collation: deleteModelOpt)
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    @objc func bulkWriteAddDeleteMany(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be a valid document")
            }
            guard let deleteModelOpt = try OptionsParser.getDeleteModelOption(call.getObject("options")) else {
                throw UserError.invalidArgumentError(message: "options (if provided) must contain a valid collation document")
            }
            
            let writeModel = MongoCollection<Document>.DeleteManyModel(filter, collation: deleteModelOpt)
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    @objc func bulkWriteAddInsertOne(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let doc = try OptionsParser.getDocument(call.getObject("doc"), "doc") else {
                throw UserError.invalidArgumentError(message: "doc must be a valid document")
            }
            
            let writeModel = MongoCollection<Document>.InsertOneModel(doc)
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    @objc func bulkWriteAddReplaceOne(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be a valid document")
            }
            guard let opts = try OptionsParser.getReplaceOneModelOptions(call.getObject("options")) else {
                throw UserError.invalidArgumentError(message: "options (if provided) must contain valid replaceOne options")
            }
            guard let replacement = try OptionsParser.getDocument(call.getObject("replacement"), "replacement") else {
                throw UserError.invalidArgumentError(message: "replacement must be a valid document")
            }
            
            let writeModel = MongoCollection<Document>.ReplaceOneModel(filter: filter,
                                                                       replacement: replacement,
                                                                       collation: opts.collation,
                                                                       upsert: opts.upsert
            )
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    @objc func bulkWriteAddUpdateOne(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be a valid document")
            }
            guard let opts = try OptionsParser.getUpdateModelOptions(call.getObject("options")) else {
                throw UserError.invalidArgumentError(message: "options (if provided) must contain valid replaceOne options")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw UserError.invalidArgumentError(message: "update must be a valid document")
            }
            
            let writeModel = MongoCollection<Document>.UpdateOneModel(filter: filter,
                                                                      update: update,
                                                                      arrayFilters: opts.arrayFilters,
                                                                      collation: opts.collation,
                                                                      upsert: opts.upsert
            )
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
    
    @objc func bulkWriteAddUpdateMany(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard var op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            guard let filter = try OptionsParser.getDocument(call.getObject("filter"), "filter") else {
                throw UserError.invalidArgumentError(message: "filter must be a valid document")
            }
            guard let opts = try OptionsParser.getUpdateModelOptions(call.getObject("options")) else {
                throw UserError.invalidArgumentError(message: "options (if provided) must contain valid replaceOne options")
            }
            guard let update = try OptionsParser.getDocument(call.getObject("update"), "update") else {
                throw UserError.invalidArgumentError(message: "update must be a valid document")
            }
            
            let writeModel = MongoCollection<Document>.UpdateManyModel(filter: filter,
                                                                       update: update,
                                                                       arrayFilters: opts.arrayFilters,
                                                                       collation: opts.collation,
                                                                       upsert: opts.upsert
            )
            
            op.addRequest(writeModel)
            
            var outData: PluginResultData = [:]
            outData["success"] = true
            call.resolve(outData)
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }

    @objc func bulkWriteCancel(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            
            var outData: PluginResultData = [:];
            outData["removed"] = self.bulkOperations.removeValue(forKey: opId)
            
            call.resolve(outData)
            
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }

    @objc func bulkWriteExecute(_ call: CAPPluginCall) {
        do {
            guard let opIdString = call.getString("operationId") else {
                throw UserError.invalidArgumentError(message: "operationId must be provided and must be the string id of a valid bulk operation")
            }
            guard let opId = UUID.init(uuidString: opIdString) else {
                throw UserError.invalidArgumentError(message: "operationId is not a valid bulk operation id format")
            }
            guard let op = self.bulkOperations[opId] else {
                throw UserError.invalidArgumentError(message: "operationId does not refer to a valid bulk operation")
            }
            
            let result = try op.execute()
            
            var outData: PluginResultData;
            if result == nil {
                let nilData: PluginResultData = [
                    "deletedCount": -1,
                    "insertedCount": -1,
                    "matchedCount": -1,
                    "modifiedCount": -1,
                    "upsertedCount": -1,
                ]
                outData = nilData
                outData["insertedIds"] = nil
                outData["upsertedIds"] = nil
            } else {
                outData = [
                    "deletedCount": result!.deletedCount,
                    "insertedCount": result!.insertedCount,
                    "insertedIds": result!.insertedIds.mapValues {bsonToJsValue($0)},
                    "matchedCount": result!.matchedCount,
                    "modifiedCount": result!.modifiedCount,
                    "upsertedCount": result!.upsertedCount,
                    "upsertedIds": result!.upsertedIds.mapValues {bsonToJsValue($0)}
                    ]
            }
            
            self.bulkOperations.removeValue(forKey: opId)
            
            call.resolve(outData)
            
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not execute findOneAndDelete")
        }
    }
}

