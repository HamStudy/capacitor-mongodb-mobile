declare module "@capacitor/core" {
  interface PluginRegistry {
    MongoDBMobile: MongoDBMobilePlugin;
  }
}

import {
  MongoMobileTypes, MongoDBMobileSource
} from './sharedDefinitions';

export {
  MongoMobileTypes
}

export interface MongoDBMobilePlugin extends MongoDBMobileSource {
}
