
import { Plugins, PluginRegistry } from '@capacitor/core';
import { MongoDBMobilePlugin } from '../definitions';
import { Collection } from './collection';

const MongoDBMobile = Plugins.MongoDBMobile as MongoDBMobilePlugin;

import {MongoMobileTypes} from '../definitions';
import { CommonOptions, WriteConcern, IndexOptions } from './commonTypes';

export interface DbCreateOptions extends CommonOptions {
  /**
   * Default: false; Force server to create _id fields instead of client.
   */
  forceServerObjectId?: boolean;
  /**
   * Serialize functions on any object.
   */
  promoteValues?: boolean;
  /**
   * Custom primary key factory to generate _id values (see Custom primary keys).
   */
  pkFactory?: object;
  /**
   * ES6 compatible promise constructor
   */
  promiseLibrary?: PromiseConstructor;
  /**
   * Sets a cap on how many operations the driver will buffer up before giving up on getting a
   * working connection, default is -1 which is unlimited.
   */
  bufferMaxEntries?: number;
}

export interface CollectionCreateOptions extends CommonOptions {
  // raw?: boolean;
  // pkFactory?: object;
  // serializeFunctions?: boolean;
  // strict?: boolean;
  capped?: boolean;
  autoIndexId?: boolean;
  size?: number;
  max?: number;
  flags?: number;
  storageEngine?: object;
  validator?: object;
  validationLevel?: "off" | "strict" | "moderate";
  validationAction?: "error" | "warn";
  indexOptionDefaults?: object;
  viewOn?: string;
  pipeline?: any[];
  collation?: MongoMobileTypes.Collation;
}


export class Db {
  private _collections: {[name: string]: Collection};
  constructor(public readonly databaseName: string, public readonly options: DbCreateOptions = {}) {
    if (!options.promiseLibrary) {
      options.promiseLibrary = Promise;
    }
  }
  collection(name: string) {
    if (!this._collections[name]) {
      this._collections[name] = new Collection(this, name);
    }
    return this._collections[name];
  }
  get collections() : Promise<Collection[]> {
    return (async () => {
      let collections = await MongoDBMobile.listCollections({db: this.databaseName});
      let collectionList = collections.map(c => this.collection(c.name));
      return collectionList;
    })();
  }

  async createCollection(name: string, options?: CollectionCreateOptions): Promise<Collection> {
    let cThingy = await MongoDBMobile.createCollection({
      db: this.databaseName,
      collection: name,
      options: <MongoMobileTypes.CollectionCreateOptions>{
        autoIndexId: options.autoIndexId,
        capped: options.capped,
        collation: options.collation,
        flags: options.flags,
        indexOptionDefaults: options.indexOptionDefaults,
        max: options.max,
        pipeline: options.pipeline,
        size: options.size,
        storageEngine: options.storageEngine,
        validationAction: options.validationAction,
        validationLevel: options.validationLevel,
        validator: options.validator,
        viewOn: options.viewOn,
        writeConcern: <MongoMobileTypes.WriteConcern>{
          w: options.w,
          wtimeout: options.wtimeout,
          j: options.j
        }
      }
    });
    return this.collection(cThingy.collection);
  }

  async command(command: object, options?: {writeConcern: WriteConcern | number}): Promise<any> {
    let result = await MongoDBMobile.runCommand({
      db: this.databaseName, command: command, options: options
    });

    return result.reply;
  }

  async dropCollection(name: string): Promise<boolean> {
    try {
      await MongoDBMobile.dropCollection({db: this.databaseName, collection: name});
      return true;
    } catch {
      return false;
    }
  }
  async dropDatabase(): Promise<boolean> {
    try {
      await MongoDBMobile.dropDatabase({db: this.databaseName});
      return true;
    } catch {
      return false;
    }
  }
}