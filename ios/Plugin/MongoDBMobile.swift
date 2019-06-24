import Foundation
import Capacitor
import StitchCore
import StitchLocalMongoDBService
import MongoSwift

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
    var bulkOperations: [UUID: BulkWriteBatch] = [:]
//
//    public override init() {
//        var bundleId = ""
//        if (bundleIdentifier != nil) {
//            bundleId = bundleIdentifier!
//        }
//        do {
//            appClient = try Stitch.initializeDefaultAppClient(
//                withClientAppID: bundleId
//            )
//            print("Initialized stich app client")
//
//            mongoClient =
//                try appClient!.serviceClient(fromFactory: mongoClientFactory)
//        } catch {
//        }
//        super.init()
//    }
    
    
    @objc func initDb(_ call: CAPPluginCall) {
        do {
            if appClient != nil {
                call.resolve([
                    "success": true
                    ]);
                return;
            }
            let appId = call.getString("appId", bundleIdentifier)
            if appId == nil {
                throw UserError.invalidArgumentError(message: "appId must be provided and must be a string (e.g. org.hamstudy.somecoolthingy)")
            }
            appClient = try Stitch.initializeDefaultAppClient(
                withClientAppID: appId!
            )
            print("Initialized stich app client")
            mongoClient =
                try appClient!.serviceClient(fromFactory: mongoClientFactory)
            call.resolve([
                "success": true
                ]);
        } catch UserError.invalidArgumentError(let message) {
            handleError(call, message)
        } catch {
            handleError(call, "Could not initialize db")
        }
    }

    /**
     * Helper for handling errors
     */
    func handleError(_ call: CAPPluginCall, _ message: String, _ error: Error? = nil) {
        call.error(message, error)
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
    
}
