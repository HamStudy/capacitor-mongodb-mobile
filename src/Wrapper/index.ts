
import { MongoDBMobilePlugin } from '../definitions';

let MongoMobilePlugin: MongoDBMobilePlugin = null;

export function getMongoMobilePlugin() {
  return MongoMobilePlugin;
}
export function setMongoMobilePlugin(plugin: MongoDBMobilePlugin) {
  MongoMobilePlugin = plugin;
}


export * from './db';
export * from './collection';
export * from './cursor';
export * from './aggregationCursor';
export * from './bulkOps';