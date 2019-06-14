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
