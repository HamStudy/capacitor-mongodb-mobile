declare module "@capacitor/core" {
  interface PluginRegistry {
    MongoDBMobile: MongoDBMobilePlugin;
  }
}

export namespace MongoMobileTypes {
    export type Document = Object;
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
        j?: boolean;
        /** how many nodes it must write to; in practice this should always be 0 or 1 in mongodb mobile */
        w?: number | 'majority' | string; // number
        /** Timeout in milliseconds after which the operation will return with an error */
        wtimeout?: number;
    }
    export interface FindOptions {
        allowPartialResults?: boolean;
        batchSize?: number;
        collation?: Collation;
        comment?: string;
        cursorType?: CursorType;
        hint?: IndexHint;
        limit?: number;
        max?: any;
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
        maxTimeMS?: number;
        limit?: number;
        writeConcern?: WriteConcern | number;
    }
    export interface InsertOneOptions {
        bypassDocumentValidation?: boolean;
        writeConcern?: WriteConcern | number;
    }
    export interface InsertManyOptions extends InsertOneOptions {
        ordered?: boolean;
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
        arrayFilters?: any[],
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
        expireAfter?: number;
        name?: string;
        sparse?: boolean;
        storageEngine?: string;
        unique?: boolean;
        version?: number;
        defaultLanguage?: string;
        languageOverride?: string;
        textVersion?: number;
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

    export interface CollectionCreateOptions {
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
      writeConcern?: WriteConcern | number;
    }

    export type PipelineStage<T extends Object> = T;
}

export interface MongoDBMobilePlugin {
  initWithId(options: {appID: string}): Promise<void>;
  listDatabases(): Promise<{name: string}[]>;
  listCollections(options: {db: string}): Promise<{name: string}[]>;
  createCollection(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.CollectionCreateOptions,
  }): Promise<{collection: string}>;
  runCommand(options: {
    db: string,
    command: any,
    options?: {
      writeConcern: MongoMobileTypes.WriteConcern | number
    }
  }): Promise<{reply: any}>;
  dropDatabase(options: {db: string}): Promise<{dropped: boolean}>;
  dropCollection(options: {db: string, collection: string}): Promise<{dropped: boolean}>;

  /***********************\
   * Query / Read methods
  \***********************/ 
  count(options: MongoMobileTypes.DatabaseDef & {
    filter: any, options?: MongoMobileTypes.CountOptions
  }): Promise<{count: number}>;
  find<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & {
      cursor?: false,
      filter: any,
      options?: MongoMobileTypes.FindOptions,
  }) : Promise<{results: T[]}>;
  find(options: MongoMobileTypes.DatabaseDef & {
      cursor: true,
      filter: any,
      options?: MongoMobileTypes.FindOptions
  }) : Promise<{cursorId: string}>;
  aggregate<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & {
    cursor?: false,
    pipeline: MongoMobileTypes.PipelineStage<{}>[],
    options?: MongoMobileTypes.AggregateOptions,
  }) : Promise<{results: T[]}>;
  aggregate(options: MongoMobileTypes.DatabaseDef & {
    cursor: true,
    pipeline: MongoMobileTypes.PipelineStage<{}>[],
    options?: MongoMobileTypes.AggregateOptions,
  }) : Promise<{cursorId: string}>;
  cursorGetNext<T extends MongoMobileTypes.Document>(options: {
    cursorId: string,
    batchSize?: number,
  }) : Promise<{results: T[], complete?: true}>;
  closeCursor(options: {
    cursorId: string,
  }) : Promise<{success: true, removed: boolean}>;

  /**********************\
   * Basic write methods
  \**********************/ 
  insertOne<T extends object>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.InsertOneOptions,
    doc: T,
  }) : Promise<{success: true, insertedId?: string}>;
  insertMany<T extends object>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.InsertManyOptions,
    docs: T[],
  }) : Promise<{success: true, insertedIds?: string[], insertedCount: number}>;
  replaceOne<T extends object>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.ReplaceOptions,
    filter: any,
    doc: T,
  }) : Promise<{success: true, upsertedId?: string, matchedCount: number, modifiedCount: number, upsertedCount: number}>;
  updateOne(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.UpdateOptions,
    filter: any,
    update: any,
  }) : Promise<MongoMobileTypes.UpdateResult>;
  updateMany(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.UpdateOptions,
    filter: any,
    update: any,
  }) : Promise<MongoMobileTypes.UpdateResult>;
  deleteOne(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DeleteOptions,
    filter: any,
  }) : Promise<{success: true, deletedCount?: number}>;
  deleteMany(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DeleteOptions,
    filter: any,
  }) : Promise<{success: true, deletedCount?: number}>;


  /************************\
   * FindAndModify methods
  \************************/ 
  findOneAndDelete<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.FindOneAndDeleteOptions,
    filter: any,
  }) : Promise<{doc: T}>;
  findOneAndReplace<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.FindOneAndReplaceOptions,
    filter: any,
    replacement: T,
  }) : Promise<{doc: T}>;
  findOneAndUpdate<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.FindOneAndUpdateOptions,
    filter: any,
    update: any,
  }) : Promise<{doc: T}>;
  

  /*********************\
   * Bulk Write methods
  \*********************/
  newBulkWrite(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.BulkWriteOptions,
  }) : Promise<{operationId: string}>;
  bulkWriteAddDeleteOne(options: {
    operationId: string,
    filter: any,
    options?: MongoMobileTypes.DeleteModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddDeleteMany(options: {
    operationId: string,
    filter: any,
    options?: MongoMobileTypes.DeleteModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddDeleteMany(options: {
    operationId: string,
    filter: any,
    options?: MongoMobileTypes.DeleteModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddInsertOne<T extends object>(options: {
    operationId: string,
    doc: T,
  }) : Promise<{success: true}>;
  bulkWriteAddReplaceOne<T extends object>(options: {
    operationId: string,
    filter: any,
    replacement: T,
    options?: MongoMobileTypes.ReplaceOneModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddUpdateOne(options: {
    operationId: string,
    filter: any,
    update: any,
    options?: MongoMobileTypes.UpdateModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteAddUpdateMany(options: {
    operationId: string,
    filter: any,
    update: any,
    options?: MongoMobileTypes.UpdateModelOptions,
  }) : Promise<{success: true}>;
  bulkWriteCancel(options: {
    operationId: string,
  }) : Promise<{removed: boolean}>;
  bulkWriteExecute(options: {
    operationId: string,
  }) : Promise<MongoMobileTypes.BulkWriteResult>;


  /***************************\
   * Index management methods
  \***************************/ 
  createIndexes(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.CreateIndexOptions,
    indexes: [any, MongoMobileTypes.IndexOptions][],
  }) : Promise<{indexesCreated: string[]}>;
  dropIndex(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DropIndexOptions,
    name: string,
  }) : Promise<any>;
  dropIndex(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DropIndexOptions,
    keys: object,
  }) : Promise<any>;
  listIndexes(options: MongoMobileTypes.DatabaseDef & {
  }) : Promise<{results: any[]}>;
}
