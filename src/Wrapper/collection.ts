import { Plugins, PluginRegistry } from '@capacitor/core';
import { MongoDBMobilePlugin, MongoMobileTypes } from '../definitions';
const MongoDBMobile = Plugins.MongoDBMobile as MongoDBMobilePlugin;

import { Db } from "./db";
import { IndexOptions, CommonOptions } from './commonTypes';

const defaultWriteConcern = 1;

function getWriteConcern(options?: CommonOptions) {
  let writeConcern: MongoMobileTypes.WriteConcern = {
    w: defaultWriteConcern
  };
  if (typeof options.w != 'undefined') {
    writeConcern.w = options.w;
  }
  if (typeof options.j != 'undefined') {
    writeConcern.j = !!options.j;
  }
  if (typeof options.wtimeout != 'undefined') {
    writeConcern.wtimeout = options.wtimeout;
  }
  return writeConcern;
}

export class Collection {
  constructor(public readonly db: Db, public collectionName: string) {
  }
  async drop(): Promise<any> {
    return MongoDBMobile.dropCollection({
      db: this.db.databaseName,
      collection: this.collectionName,
    });
  }
  async createIndex(name: string, fieldOrSpec: string | object, options?: IndexOptions): Promise<any> {
    let indexRes = await MongoDBMobile.createIndexes({
      db: this.db.databaseName,
      collection: this.collectionName,
      indexes: [
        [fieldOrSpec, {
          background: options.background,
          collation: options.collation,
          defaultLanguage: options.default_language,
          expireAfter: options.expireAfterSeconds,
          max: options.max,
          min: options.min,
          name: options.name,
          partialFilterExpression: options.partialFilterExpression,
          sparse: options.sparse,
          unique: options.unique,
          version: options.v,
        }]
      ],
      options: {
        writeConcern: getWriteConcern(options)
      }
    });
    
    return indexRes && indexRes[0];
  }

  async dropIndex(indexName: string, options?: CommonOptions & { maxTimeMS?: number }): Promise<any> {
    let res = await MongoDBMobile.dropIndex({
      db: this.db.databaseName, collection: this.collectionName,
      name: indexName,
      options: {
        writeConcern: getWriteConcern(options)
      }
    });

    return res;
  }
}