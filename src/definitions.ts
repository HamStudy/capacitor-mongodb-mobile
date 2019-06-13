declare module "@capacitor/core" {
  interface PluginRegistry {
    MongoDBMobile: MongoDBMobilePlugin;
  }
}

declare namespace MongoDBMobile {
    export interface Document {
        _id: string;
    }
    export type TypedDocument<T> = Document & T;
    export interface DatabaseDef {
        db: string;
        collection: string;
    }
    export type IndexFields = {[field: string]: 1 | -1};
    export type IndexHint = string | IndexFields;
    export interface CountOptions {
        collation?: Collation;
        hint?: IndexHint;
        limit?: number;
        maxTimeMS?: number;
        skip?: number;
    }
    export enum CursorType {
        Tailable = 'tailable',
        NonTailable = 'nonTailable',
        TailableAwait = 'tailableAwait',
    }
    export interface WriteConcern {
        /** True if it needs to write to the journal before returning */
        j: boolean;
        /** how many nodes it must write to; in practice this should always be 0 or 1 in mongodb mobile */
        w: 0 | 1; // number
        /** Timeout in milliseconds after which the operation will return with an error */
        wtimeout: number;
    }
    export interface FindOptions {
        allowPartialResults?: boolean;
        batchSize?: number;
        collation?: Collation;
        comment?: string;
        cursorType?: CursorType;
        hint?: IndexHint;
        limit?: number;
        /**
         * I don't actually know what this is for
         */
        max?: any;
        /**
         * I don't actually know what this is for
         */
        min?: any;

        maxScan?: number;
        maxTimeMS?: number;
        noCursorTimeout?: boolean;
        projection?: any;
        returnKey?: boolean;
        showRecordId?: boolean;
        skip?: number;
        sort?: IndexFields;
    }
    export interface AggregateOptions {
        allowDiskUse?: boolean;
        batchSize?: number;
        bypassDocumentValidation?: boolean;
        collation?: Collation;
        comment?: string;
        hint?: IndexHint;
        limit?: number;
        writeConcern?: WriteConcern | number;
    }
    export interface InsertOneOptions {
        bypassDocumentValidation?: boolean;
    }
    export interface InsertManyOptions {
        bypassDocumentValidation?: boolean;
        ordered?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface ReplaceOptions {
        bypassDocumentValidation?: boolean;
        collation?: Collation;
        upsert?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface UpdateOptions {
        arrayFilters: any[],
        bypassDocumentValidation?: boolean;
        collation?: Collation;
        upsert?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface UpdateResult {
        success: true;
        upsertedId?: string;
        matchedCount: number;
        modifiedCount: number;
        upsertedCount: number;
    }
    export interface DeleteOptions {
        collation?: Collation;
        writeConcern?: WriteConcern | number;
    }
    export interface FindOneAndDeleteOptions {
        collation?: Collation;
        maxTimeMS?: number;
        projection?: any;
        sort?: IndexFields;
        writeConcern?: WriteConcern | number;
    }
    export interface FindOneAndReplaceOptions {
        bypassDocumentValidation?: boolean;
        collation?: Collation;
        maxTimeMS?: number;
        projection?: any;
        returnNewDocument?: boolean;
        sort?: IndexFields;
        upsert?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface FindOneAndUpdateOptions extends FindOneAndReplaceOptions {
        arrayFilters?: any[];
    }
    export interface BulkWriteOptions {
        bypassDocumentValidation?: boolean;
        ordered?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface DeleteModelOptions {
        collation?: Collation;
    }
    export interface ReplaceOneModelOptions {
        collation?: Collation;
        upsert?: boolean;
    }
    export interface UpdateModelOptions {
        arrayFilters: any[],
        collation?: Collation;
        upsert?: boolean;
    }
    export interface Collation {
        locale: string;
        caseLevel?: boolean;
        caseFirst?: string;
        strength?: number;
        numericOrdering?: boolean;
        alternate?: string;
        maxVariable?: string;
        backwards?: boolean;
    }
    export interface BulkWriteResult {
        deletedCount: number;
        insertedCount: number;
        matchedCount: number;
        modifiedCount: number;
        upsertedCount: number;
        insertedIds?: string[];
        upsertedIds?: string[];
    }

    export interface IndexOptions {
        background?: boolean;
        expireAfter: number;
        name?: string;
        sparse?: boolean;
        storageEngine?: string;
        unique?: boolean;
        version?: number;
        defaultLanguage?: string;
        languageOverride?: string;
        textVersion: number;
        weights?: any;
        sphereVersion?: number;
        bits?: number;
        max?: number;
        min?: number;
        bucketSize?: number;
        partialFilterExpression?: any;
        collation?: Collation;
    }
    export interface CreateIndexOptions {
        writeConcern?: WriteConcern | number;
    }
    export type DropIndexOptions = CreateIndexOptions;

    export type PipelineStage<T extends Object> = T;
}

export interface MongoDBMobilePlugin {
  initWithId(options: {appID: string}): Promise<void>;
  listDatabases(): Promise<MongoDBMobile.TypedDocument<any>[]>;

  /***********************\
   * Query / Read methods
  \***********************/ 
  count(options: MongoDBMobile.DatabaseDef & {
    filter: any, options?: MongoDBMobile.CountOptions
  }): Promise<{count: number}>;
  find<T extends MongoDBMobile.Document>(options: MongoDBMobile.DatabaseDef & {
      cursor?: false,
      filter: any,
      options?: MongoDBMobile.FindOptions,
  }) : Promise<{results: T[]}>;
  find(options: MongoDBMobile.DatabaseDef & {
      cursor: true,
      filter: any,
      options?: MongoDBMobile.FindOptions
  }) : Promise<{cursorId: string}>;
  aggregate<T extends MongoDBMobile.Document>(options: MongoDBMobile.DatabaseDef & {
    cursor?: false,
    pipeline: MongoDBMobile.PipelineStage<{}>[],
    options?: MongoDBMobile.AggregateOptions,
  }) : Promise<{results: T[]}>;
  aggregate(options: MongoDBMobile.DatabaseDef & {
    cursor: true,
    filter: any,
    options?: MongoDBMobile.AggregateOptions
  }) : Promise<{cursorId: string}>;
  cursorGetNext<T extends MongoDBMobile.Document>(options: {
    cursorId: string,
    batchSize?: number,
  }) : Promise<{results: T[], complete?: true}>;
  closeCursor(options: {
    cursorId: string,
  }) : Promise<{success: true, removed: boolean}>;

  /**********************\
   * Basic write methods
  \**********************/ 
  insertOne<T extends object>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.InsertOneOptions,
    doc: T,
  }) : Promise<{success: true, insertedId?: string}>;
  insertMany<T extends object>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.InsertManyOptions,
    docs: T[],
  }) : Promise<{success: true, insertedIds?: string[], insertedCount: number}>;
  replaceOne<T extends object>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.ReplaceOptions,
    filter: any,
    doc: T,
  }) : Promise<{success: true, upsertedId?: string, matchedCount: number, modifiedCount: number, upsertedCount: number}>;
  updateOne(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.UpdateOptions,
    filter: any,
    update: any,
  }) : Promise<MongoDBMobile.UpdateResult>;
  updateMany(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.UpdateOptions,
    filter: any,
    update: any,
  }) : Promise<MongoDBMobile.UpdateResult>;
  deleteOne(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.DeleteOptions,
    filter: any,
  }) : Promise<{success: true, deletedCount?: number}>;
  deleteMany(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.DeleteOptions,
    filter: any,
  }) : Promise<{success: true, deletedCount?: number}>;


  /************************\
   * FindAndModify methods
  \************************/ 
  findOneAndDelete<T extends MongoDBMobile.Document>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.FindOneAndDeleteOptions,
    filter: any,
  }) : Promise<{doc: T}>;
  findOneAndReplace<T extends MongoDBMobile.Document>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.FindOneAndReplaceOptions,
    filter: any,
    replacement: T,
  }) : Promise<{doc: T}>;
  findOneAndUpdate<T extends MongoDBMobile.Document>(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.FindOneAndUpdateOptions,
    filter: any,
    update: any,
  }) : Promise<{doc: T}>;
  

  /*********************\
   * Bulk Write methods
  \*********************/
  newBulkWrite(options: MongoDBMobile.DatabaseDef & {
    options?: MongoDBMobile.FindOneAndUpdateOptions,
  }) : Promise<{operationId: string}>;
  bulkWriteAddDeleteOne(options: {
    operationId: string,
    filter: any,
    options?: MongoDBMobile.DeleteModelOptions,
  }) : Promise<{operationId: string}>;
  bulkWriteAddDeleteMany(options: {
    operationId: string,
    filter: any,
    options?: MongoDBMobile.DeleteModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddDeleteMany(options: {
    operationId: string,
    filter: any,
    options?: MongoDBMobile.DeleteModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddInsertOne<T extends object>(options: {
    operationId: string,
    doc: T,
  }) : Promise<{success: true}>;
  bulkWriteAddReplaceOne<T extends object>(options: {
    operationId: string,
    filter: any,
    replacement: T,
    options?: MongoDBMobile.ReplaceOneModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddUpdateOne(options: {
    operationId: string,
    filter: any,
    update: any,
    options?: MongoDBMobile.UpdateModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddUpdateMany(options: {
    operationId: string,
    filter: any,
    update: any,
    options?: MongoDBMobile.UpdateModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteCancel(options: {
    operationId: string,
  }) : Promise<{removed: boolean}>;
  bulkWriteExecute(options: {
    operationId: string,
  }) : Promise<MongoDBMobile.BulkWriteResult>;
}
