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
}
