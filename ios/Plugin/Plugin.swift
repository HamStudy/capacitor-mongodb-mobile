import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

//var dbInstances: [String, MongoDatabase] = []

enum MDBAPIError : Error {
    case invalidArguments(message: String)
}

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(MongoDBMobile)
public class MongoDBMobile: CAPPlugin {
    var bundleIdentifier = Bundle.main.bundleIdentifier
    var appClient: StitchAppClient?;
    var mongoClient: MongoClient?;
    
    var cursorMap: [UUID: MongoCursor<Document>] = [:]
    
    public override init() {
        var bundleId = ""
        if (bundleIdentifier != nil) {
            bundleId = bundleIdentifier!
        }
        do {
            appClient = try Stitch.initializeDefaultAppClient(
                withClientAppID: bundleId
            )
            print("Initialized stich app client")
            
            mongoClient =
                try appClient!.serviceClient(fromFactory: mongoClientFactory)
        } catch {
        }
        super.init()
    }
    
    /**
     * Helper for handling errors
     */
    func handleError(_ call: CAPPluginCall, _ message: String, _ error: Error? = nil) {
        call.error(message, error)
    }
    
    func returnCursor(_ call: CAPPluginCall, cursor: MongoCursor<Document>) {
        let cursorId = UUID()
        
        cursorMap[cursorId] = cursor
        
        call.resolve([
            "cursorId": cursorId.uuidString
            ])
    }
    func returnDocsFromCursor(_ call: CAPPluginCall, cursor: MongoCursor<Document>) {
        var resultsJson: [Any] = []
        for doc in resultsDocuments {
            resultsJson.append(convertToDictionary(text: doc.extendedJSON)!)
        }
        call.resolve(["results": resultsJson])
    }
    
    @objc func initWithId(_ call: CAPPluginCall) {
        do {
            appClient = try Stitch.initializeDefaultAppClient(
                withClientAppID: call.getString("appID")!
            )
            print("Initialized stich app client")
            mongoClient =
                try appClient!.serviceClient(fromFactory: mongoClientFactory)
        } catch {
        }
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
            guard let queryDoc = try OptionsParser.getDocument(query, "query") else {
                throw UserError.invalidArgumentError(message: "query must be provided and must be an object")
            }
            
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
        guard let pipelineAny = call.getArray("pipeline", Any.self) else {
            throw MDBAPIError.invalidArguments(message: "pipeline must be provided and must be an array of objects")
        }
        var pipeline: [Document] = []
        for (i, step) in pipelineAny.enumerated() {
            guard let doc = try OptionsParser.getDocument(step, "pipeline[\(i)]") else {
                throw UserError.invalidArgumentError(message: "Invalid type for variable pipeline[\(i)]: expected document")
            }
            pipeline.append(doc)
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
    
    @objc func execAggregate(_ call: CAPPluginCall) {
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
    
    // Credit to @ayaio on stackoverflow.com for this function.
    func convertToDictionary(text: String) -> [String: Any]? {
        if let data = text.data(using: .utf8) {
            do {
                return try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
            } catch {
                print(error.localizedDescription)
            }
        }
        return nil
    }
}
