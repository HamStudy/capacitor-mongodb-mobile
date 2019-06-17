// TODO: This needs to be fixed to a real error; this will require some special handling, but
// at present I don't even know what errors can be thrown.
export type MongoError = Error;

export interface IteratorCallback<T> {
    (doc: T): void;
}
export interface EndCallback {
    (error: MongoError): void;
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