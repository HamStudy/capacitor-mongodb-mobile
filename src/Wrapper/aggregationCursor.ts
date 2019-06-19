import { MongoMobileTypes } from '../definitions';
import { Collection } from './collection';
import { IteratorCallback } from './commonTypes';
import { getMongoMobilePlugin, CursorResult } from './index';

const K_AUTOCLOSE_TIMEOUT = 1000 * 30; // Automatically close the cursor after 30 seconds of inactivity if it isn't closed automatically

class AggregationCursor<T extends Object> {
  private cursorId: string = null;
  private _asyncTimeoutMs = K_AUTOCLOSE_TIMEOUT;
  private _curBatch: T[] = [];
  private _isEnd = false;
  private _isClosed = false;

  private autocloseTimer: ReturnType<typeof setTimeout> = null;

  active() { return !!this.cursorId; }
  private resetCloseTimer() {
    if (this.autocloseTimer !== null) {
      clearTimeout(this.autocloseTimer);
    }
    this.autocloseTimer = setTimeout(() => this.close(), this._asyncTimeoutMs);
  }
  private errorIfClosed() {
    if (this.isClosed()) { throw new Error("Attempting to access a closed cursor"); }
  }
  private async execute() {
    let cursorStart = await getMongoMobilePlugin().aggregate({
      db: this.collection.db.databaseName,
      collection: this.collection.collectionName,

      pipeline: this._pipelineStages,
      options: this.options,
      cursor: true
    });
    this.cursorId = cursorStart.cursorId;
    this.resetCloseTimer();
  }

  constructor(private collection: Collection, private _pipelineStages: any[] = [], private options: MongoMobileTypes.AggregateOptions = {}) {
  }

  allowDiskUse(value: boolean): AggregationCursor<T> {
    this.options.allowDiskUse = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#batchSize */
  batchSize(value: number): AggregationCursor<T> {
    this.errorIfClosed();
    if (value < 1) { throw new Error("batchSize must be at least 1"); }
    this.options.batchSize = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#clone */
  clone(): AggregationCursor<T> {
    let nc = new AggregationCursor<T>(this.collection, this._pipelineStages);
    nc.options = this.options;
    nc._asyncTimeoutMs = this._asyncTimeoutMs;
    return nc;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#close */
  async close(): Promise<CursorResult> {
    if (!this.cursorId) { return false; }
    await getMongoMobilePlugin().closeCursor({
      cursorId: this.cursorId
    });
    if (this.autocloseTimer !== null) {
      clearTimeout(this.autocloseTimer);
      this.cursorId = null;
      this.autocloseTimer = null;
    }
    this._isClosed = true;
    return true;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#collation */
  collation(value: MongoMobileTypes.Collation): AggregationCursor<T> {
    this.options.collation = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#explain */
  explain(): Promise<CursorResult> {
    throw new Error("Not implemented!");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#each */
  async each(iterator: IteratorCallback<T>) : Promise<void> {
    this.errorIfClosed();
    let next: T;
    while (next = await this.next()) {
      iterator(next);
    }
    await this.close();
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#hasNext */
  hasNext(): Promise<boolean> {
    this.errorIfClosed();
    // TODO implement
    throw new Error("Not implemented");
  }
  geoNear(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $geoNear: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#group */
  group(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $group: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#limit */
  limit(value: number): AggregationCursor<T> {
    this.options.limit = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#match */
  match(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $match: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#maxTimeMS */
  maxTimeMS(value: number): AggregationCursor<T> {
    this.options.maxTimeMS = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#next */
  async next(): Promise<T | null> {
    if (this._isClosed) {
      return null;
    }
    if (!this.cursorId){
      await this.execute();
    } else {
      this.resetCloseTimer();
    }
    if (this._curBatch.length) {
      return this._curBatch.shift();
    } else if (this._isEnd) {
      this.close();
      return null;
    }

    let next = await getMongoMobilePlugin().cursorGetNext<T>({
      cursorId: this.cursorId, batchSize: this.options.batchSize || 5
    });
    if (next.complete) {
      this._isEnd;
    }
    this._curBatch = next.results || [];
    
    return this.next();
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#out */
  out(destination: string): AggregationCursor<T> {
    this._pipelineStages.push({
      $out: destination
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#project */
  project(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $project: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#redact */
  redact(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $redact: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#rewind */
  async rewind() {
    if (!this._isClosed) {
      this.close();
    }
    this._isClosed = false;
    this._isEnd = false;
    this._curBatch = [];
    this.cursorId = null;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#setEncoding */
  skip(value: number): AggregationCursor<T> {
    this._pipelineStages.push({
      $skip: value
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#sort */
  sort(document: object): AggregationCursor<T> {
    this._pipelineStages.push({
      $sort: document
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#toArray */
  async toArray(): Promise<T[]> {
    this.errorIfClosed();
    let all: T[] = [];
    let next: T;
    while (next = await this.next()) {
      all.push(next);
    }
    return all;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/AggregationCursor.html#unwind */
  unwind(field: string): AggregationCursor<T> {
    this._pipelineStages.push({
      $unwind: field
    });
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#isClosed */
  isClosed() {
    return this._isClosed;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#limit */
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#map */
  map<U>(transform: (document: T) => U): AggregationCursor<U> {
    throw new Error("Not implemented");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#maxAwaitTimeMS */
  maxAwaitTimeMS(value: number): AggregationCursor<T> {
    this._asyncTimeoutMs = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#setReadPreference */
  setReadPreference(readPreference: any): AggregationCursor<T> {
    return this; // Total no-op since read preference is meaningless in mongodb mobile
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#toArray */
}

export { AggregationCursor };
