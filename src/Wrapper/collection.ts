import { MongoMobileTypes } from '../definitions';
import { getMongoMobilePlugin } from './index';

import { Db } from "./db";
import { IndexOptions, CommonOptions, CollectionInsertManyOptions, InsertWriteOpResult, CollectionInsertOneOptions, InsertOneWriteOpResult, UpdateQuery, FilterQuery, UpdateManyOptions, UpdateOneOptions, FindOneAndDeleteOption, UpdateWriteOpResult, DeleteWriteOpResultObject, FindAndModifyWriteOpResultObject, FindOneAndReplaceOption, FindOneAndUpdateOption } from './commonTypes';
import { AggregationCursor } from './aggregationCursor';
import { Cursor, CursorCommentOptions } from './cursor';
import { OrderedBulkOperation } from './bulkOps/OrderedBulkOperation';
import { UnorderedBulkOperation } from './bulkOps/UnorderedBulkOperation';

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
  writeConcern: MongoMobileTypes.WriteConcern = {
    w: 1, j: true
  };
  constructor(public readonly db: Db, public collectionName: string) {
  }

  async drop(): Promise<any> {
    return getMongoMobilePlugin().dropCollection({
      db: this.db.databaseName,
      collection: this.collectionName,
    });
  }
  async createIndex(name: string, fieldOrSpec: string | object, options?: IndexOptions): Promise<any> {
    let indexRes = await getMongoMobilePlugin().createIndexes({
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
    
    return indexRes && indexRes.indexesCreated && indexRes.indexesCreated[0];
  }
  aggregate<T extends object = any>(pipeline?: object[], options?: MongoMobileTypes.AggregateOptions): AggregationCursor<T> {
    return new AggregationCursor<T>(this, pipeline, options);
  }
  find<T extends object = any>(query: object, options?: MongoMobileTypes.FindOptions): Cursor<T> {
    return new Cursor<T>(this, query, options);
  }
  async findOne<T extends object = any>(query: object, options?: MongoMobileTypes.FindOptions): Promise<T | null> {
    options.limit = 1;
    options.batchSize = 1;
    let f = await this.find(query, options).batchSize(1).toArray();
    return f[0] || null;
  }
  count(query?: any, options?: CursorCommentOptions): Promise<number> {
    let cursor = new Cursor(this, query);
    return cursor.count(true, options);
  }

  async dropIndex(indexName: string, options?: CommonOptions & { maxTimeMS?: number }): Promise<any> {
    let res = await getMongoMobilePlugin().dropIndex({
      db: this.db.databaseName, collection: this.collectionName,
      name: indexName,
      options: {
        writeConcern: getWriteConcern(options)
      }
    });

    return res;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#initializeOrderedBulkOp */
  initializeOrderedBulkOp(options?: CommonOptions): OrderedBulkOperation {
    return new OrderedBulkOperation(this, {writeConcern: getWriteConcern(options)});
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#initializeUnorderedBulkOp */
  initializeUnorderedBulkOp(options?: CommonOptions): UnorderedBulkOperation {
    return new UnorderedBulkOperation(this, {writeConcern: getWriteConcern(options)});
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#insertMany */
  async insertMany(docs: any[], options: CollectionInsertManyOptions = {}): Promise<InsertWriteOpResult> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().insertMany({
      db: this.db.databaseName,
      collection: this.collectionName,
      docs: docs,
      options: {
        bypassDocumentValidation: options.bypassDocumentValidation,
        ordered: options.ordered,
        writeConcern
      }
    });

    return {
      insertedCount: res.insertedCount,
      insertedIds: res.insertedIds,
      ops: null,
      connection: this.db,
      result: {ok: res.insertedCount, n: docs.length}
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#insertOne */
  async insertOne(doc: any, options?: CollectionInsertOneOptions): Promise<InsertOneWriteOpResult> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().insertOne({
      db: this.db.databaseName,
      collection: this.collectionName,
      doc: doc,
      options: {
        bypassDocumentValidation: options.bypassDocumentValidation,
        writeConcern
      }
    });

    return {
      insertedCount: 1,
      insertedId: res.insertedId,
      ops: null,
      connection: this.db,
      result: {ok: 1, n: 1}
    };
  }
  async updateMany<D extends object>(filter: FilterQuery<D>, update: UpdateQuery<D> | D, options?: UpdateManyOptions): Promise<UpdateWriteOpResult> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().updateMany({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      update: update,
      options: {
        arrayFilters: options.arrayFilters,
        upsert: options.upsert,
        bypassDocumentValidation: options.bypassDocumentValidation,
        writeConcern
      }
    });

    return {
      connection: this.db,
      result: {
        ok: res.matchedCount,
        n: res.matchedCount,
        nModified: res.modifiedCount + res.upsertedCount,
      },
      matchedCount: res.matchedCount,
      modifiedCount: res.modifiedCount,
      upsertedCount: res.upsertedCount,
      upsertedId: res.upsertedId as any
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#updateOne */
  async updateOne<TSchema extends object>(filter: FilterQuery<TSchema>, update: UpdateQuery<TSchema> | TSchema, options?: UpdateOneOptions): Promise<UpdateWriteOpResult> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().updateOne({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      update: update,
      options: {
        arrayFilters: options.arrayFilters,
        upsert: options.upsert,
        bypassDocumentValidation: options.bypassDocumentValidation,
        writeConcern
      }
    });

    return {
      connection: this.db,
      result: {
        ok: res.matchedCount,
        n: res.matchedCount,
        nModified: res.modifiedCount + res.upsertedCount,
      },
      matchedCount: res.matchedCount,
      modifiedCount: res.modifiedCount,
      upsertedCount: res.upsertedCount,
      upsertedId: res.upsertedId as any
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#deleteMany */
  async deleteMany<TSchema extends object>(filter: FilterQuery<TSchema>, options?: CommonOptions): Promise<DeleteWriteOpResultObject> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().deleteMany({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      options: {
        writeConcern
      }
    });

    return {
      connection: this.db,
      result: {
        ok: res.deletedCount,
        n: res.deletedCount,
      },
      deletedCount: res.deletedCount
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#deleteOne */
  async deleteOne<TSchema extends object>(filter: FilterQuery<TSchema>, options?: CommonOptions ): Promise<DeleteWriteOpResultObject> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().deleteOne({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      options: {
        writeConcern
      }
    });

    return {
      connection: this.db,
      result: {
        ok: res.deletedCount,
        n: res.deletedCount,
      },
      deletedCount: res.deletedCount
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndDelete */
  async findOneAndDelete<TSchema extends object>(filter: FilterQuery<TSchema>, options?: FindOneAndDeleteOption): Promise<FindAndModifyWriteOpResultObject<TSchema>> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().findOneAndDelete({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      options: {
        writeConcern,
        collation: options.collation,
        maxTimeMS: options.maxTimeMS,
        projection: options.projection,
        sort: options.sort as any
      }
    });

    return {
      value: res.doc as TSchema,
      ok: 1
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndReplace */
  async findOneAndReplace<TSchema extends object>(filter: FilterQuery<TSchema>, replacement: TSchema, options?: FindOneAndReplaceOption): Promise<FindAndModifyWriteOpResultObject<TSchema>> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().findOneAndReplace({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      replacement: replacement,
      options: {
        writeConcern,
        bypassDocumentValidation: options.bypassDocumentValidation,
        collation: options.collation,
        maxTimeMS: options.maxTimeMS,
        projection: options.projection,
        sort: options.sort as any,
        returnNewDocument: !options.returnOriginal,
        upsert: options.upsert,
      }
    });

    return {
      value: res.doc as TSchema,
      ok: 1
    };
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndUpdate */
  async findOneAndUpdate<TSchema extends object>(filter: FilterQuery<TSchema>, update: UpdateQuery<TSchema> | TSchema, options?: FindOneAndUpdateOption): Promise<FindAndModifyWriteOpResultObject<TSchema>> {
    let writeConcern = getWriteConcern(options);

    let res = await getMongoMobilePlugin().findOneAndUpdate({
      db: this.db.databaseName,
      collection: this.collectionName,
      filter: filter,
      update: update,
      options: {
        writeConcern,
        bypassDocumentValidation: options.bypassDocumentValidation,
        collation: options.collation,
        maxTimeMS: options.maxTimeMS,
        projection: options.projection,
        sort: options.sort as any,
        returnNewDocument: !options.returnOriginal,
        upsert: options.upsert,
        arrayFilters: options.arrayFilters,
      }
    });

    return {
      value: res.doc as TSchema,
      ok: 1
    };
  }
}