// TODO: This needs to be fixed to a real error; this will require some special handling, but
// at present I don't even know what errors can be thrown.
export type MongoError = Error;

export type ObjectID = any;

export interface IteratorCallback<T> {
    (doc: T): void;
}
export interface EndCallback {
    (error: MongoError): void;
}
export interface MongoCallback<T> {
    (error: MongoError, result: T): void;
}

export interface WriteConcern {
  /**
   * requests acknowledgement that the write operation has
   * propagated to a specified number of mongod hosts
   * @default 1
   */
  w?: number | 'majority' | string;
  /**
   * requests acknowledgement from MongoDB that the write operation has
   * been written to the journal
   * @default false
   */
  j?: boolean;
  /**
   * a time limit, in milliseconds, for the write concern
   */
  wtimeout?: number;
}
export interface CommonOptions extends WriteConcern {
  // session?: ClientSession; // Not applicable to mobile
}

export interface CollationDocument {
    locale: string;
    strength?: number;
    caseLevel?: boolean;
    caseFirst?: string;
    numericOrdering?: boolean;
    alternate?: string;
    maxVariable?: string;
    backwards?: boolean;
    normalization?: boolean;

}

export interface IndexOptions extends CommonOptions {
    /**
     * Creates an unique index.
     */
    unique?: boolean;
    /**
     * Creates a sparse index.
     */
    sparse?: boolean;
    /**
     * Creates the index in the background, yielding whenever possible.
     */
    background?: boolean;
    /**
     * A unique index cannot be created on a key that has pre-existing duplicate values.
     *
     * If you would like to create the index anyway, keeping the first document the database indexes and
     * deleting all subsequent documents that have duplicate value
     */
    // dropDups?: boolean; // Does not seem to be in MongoDBMobile
    /**
     * For geo spatial indexes set the lower bound for the co-ordinates.
     */
    min?: number;
    /**
     * For geo spatial indexes set the high bound for the co-ordinates.
     */
    max?: number;
    /**
     * Specify the format version of the indexes.
     */
    v?: number; // called version in mongodb mobile
    /**
     * Allows you to expire data on indexes applied to a data (MongoDB 2.2 or higher)
     */
    expireAfterSeconds?: number; // called expireAfter in mongodb mobile
    /**
     * Override the auto generated index name (useful if the resulting name is larger than 128 bytes)
     */
    name?: string;
    /**
     * Creates a partial index based on the given filter object (MongoDB 3.2 or higher)
     */
    partialFilterExpression?: any;
    collation?: CollationDocument;
    default_language?: string; // called defaultLanguage in mongodb mobile
}

export interface CollectionInsertOneOptions extends CommonOptions {
    /**
     * Allow driver to bypass schema validation in MongoDB 3.2 or higher.
     */
    bypassDocumentValidation?: boolean;
}

export interface CollectionInsertManyOptions extends CollectionInsertOneOptions {
    /**
     * If true, when an insert fails, don't execute the remaining writes. If false, continue with remaining inserts when one fails.
     */
    ordered?: boolean;
}

export interface InsertWriteOpResult {
    insertedCount: number;
    ops: any[];
    insertedIds: { [key: number]: ObjectID };
    connection: any;
    result: { ok: number, n: number };
}
export interface InsertOneWriteOpResult {
    insertedCount: number;
    ops: any[];
    insertedId: ObjectID;
    connection: any;
    result: { ok: number, n: number };
}

export type FilterQuery<T> = {
    [P in keyof T]?: T[P] | Condition<T, P>;
} | { [key: string]: any };

export type Condition<T, P extends keyof T> = {
    $eq?: T[P];
    $gt?: T[P];
    $gte?: T[P];
    $in?: Array<T[P]>;
    $lt?: T[P];
    $lte?: T[P];
    $ne?: T[P];
    $nin?: Array<T[P]>;
    $and?: Array<FilterQuery<T[P]> | T[P]>;
    $or?: Array<FilterQuery<T[P]> | T[P]>;
    $not?: Array<FilterQuery<T[P]> | T[P]> | T[P];
    $expr?: any;
    $jsonSchema?: any;
    $mod?: [number, number];
    $regex?: RegExp;
    $options?: string;
    $text?: {
        $search: string;
        $language?: string;
        $caseSensitive?: boolean;
        $diacraticSensitive?: boolean;
    };
    $where?: object;
    $geoIntersects?: object;
    $geoWithin?: object;
    $near?: object;
    $nearSphere?: object;
    $elemMatch?: object;
    $size?: number;
    $bitsAllClear?: object;
    $bitsAllSet?: object;
    $bitsAnyClear?: object;
    $bitsAnySet?: object;
    [key: string]: any;
};

/** https://docs.mongodb.com/manual/reference/operator/update */
export type UpdateQuery<T> = {
  $inc?: { [P in keyof T]?: number } | { [key: string]: number };
  $min?: { [P in keyof T]?: number } | { [key: string]: number };
  $max?: { [P in keyof T]?: number } | { [key: string]: number };
  $mul?: { [P in keyof T]?: number } | { [key: string]: number };
  $set?: Partial<T> | { [key: string]: any };
  $setOnInsert?: Partial<T> | { [key: string]: any };
  $unset?: { [P in keyof T]?: '' } | { [key: string]: '' };
  $rename?: { [key: string]: keyof T } | { [key: string]: string };
  $currentDate?: { [P in keyof T]?: (true | { $type: 'date' | 'timestamp' }) } | { [key: string]: (true | { $type: 'date' | 'timestamp' }) };
  $addToSet?: { [P in keyof T]?: any } | { [key: string]: any };
  $pop?: { [P in keyof T]?: -1 | 1 } | { [key: string]: -1 | 1 };
  $pull?: { [P in keyof T]?: any } | { [key: string]: any };
  $push?: Partial<T> | { [key: string]: any };
  $pushAll?: Partial<T> | { [key: string]: any[] };
  $each?: Partial<T> | { [key: string]: any[] };
  $bit?: { [P in keyof T]?: any } | { [key: string]: any };
};

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#replaceOne */
export interface ReplaceOneOptions extends CommonOptions {
    upsert?: boolean;
    bypassDocumentValidation?: boolean;
}

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#updateOne */
export interface UpdateOneOptions extends ReplaceOneOptions {
    arrayFilters?: object[];
}

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#updateMany */
export interface UpdateManyOptions extends CommonOptions {
    upsert?: boolean;
    arrayFilters?: object[];
    bypassDocumentValidation?: boolean;
}

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndDelete */
export interface FindOneAndDeleteOption extends CommonOptions {
    projection?: object;
    sort?: object;
    maxTimeMS?: number;
    // session?: ClientSession;
    collation?: CollationDocument;
}
export interface UpdateWriteOpResult {
  result: { ok: number, n: number, nModified: number };
  connection: any;
  matchedCount: number;
  modifiedCount: number;
  upsertedCount: number;
  upsertedId: { _id: ObjectID };
}

export interface DeleteWriteOpResultObject {
  //The raw result returned from MongoDB, field will vary depending on server version.
  result: {
      //Is 1 if the command executed correctly.
      ok?: number;
      //The total count of documents deleted.
      n?: number;
  };
  //The connection object used for the operation (the db object in this case).
  connection?: any;
  //The number of documents deleted.
  deletedCount?: number;
}
/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#~findAndModifyWriteOpResult */
export interface FindAndModifyWriteOpResultObject<TSchema extends object = any> {
    //Document returned from findAndModify command.
    value?: TSchema;
    //The raw lastErrorObject returned from the command.
    lastErrorObject?: any;
    //Is 1 if the command executed correctly.
    ok?: number;
}

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndReplace */
export interface FindOneAndReplaceOption extends CommonOptions {
    bypassDocumentValidation?: boolean;
    projection?: object;
    sort?: object;
    maxTimeMS?: number;
    upsert?: boolean;
    returnOriginal?: boolean;
    collation?: CollationDocument;
}

/** http://mongodb.github.io/node-mongodb-native/3.1/api/Collection.html#findOneAndUpdate */
export interface FindOneAndUpdateOption extends FindOneAndReplaceOption {
    arrayFilters?: object[];
}
