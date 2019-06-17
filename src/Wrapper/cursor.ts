import { Plugins, PluginRegistry } from '@capacitor/core';
import { MongoDBMobilePlugin, MongoMobileTypes } from '../definitions';
import { Db } from './db';
import { Collection } from './collection';
import { IteratorCallback, EndCallback } from './commonTypes';
const MongoDBMobile = Plugins.MongoDBMobile as MongoDBMobilePlugin;
export type CursorResult = object | null | boolean;

const K_AUTOCLOSE_TIMEOUT = 1000 * 30; // Automatically close the cursor after 30 seconds of inactivity if it isn't closed automatically

export interface CursorCommentOptions {
    skip?: number;
    limit?: number;
    maxTimeMS?: number;
    hint?: string;
    readPreference?: any; // ignored
}
class Cursor<T> {
  private cursorId: string = null;
  private options: MongoMobileTypes.FindOptions = {};
  private _batchSize: number = 100;
  private _curBatch: T[] = [];
  private _isEnd = false;
  private _isClosed = false;

  private autocloseTimer: ReturnType<typeof setTimeout> = null;

  active() { return !!this.cursorId; }
  private resetCloseTimer() {
    if (this.autocloseTimer !== null) {
      clearTimeout(this.autocloseTimer);
    }
    this.autocloseTimer = setTimeout(() => this.close(), K_AUTOCLOSE_TIMEOUT);
  }
  private async execute() {
    let cursorStart = await MongoDBMobile.find({
      db: this.collection.db.databaseName,
      collection: this.collection.collectionName,
      filter: this._filter,
      options: this.options,
      cursor: true
    });
    this.cursorId = cursorStart.cursorId;
    this.resetCloseTimer();
  }

  static fromCursor<U>(cursor: Cursor<U>) {
    // // let n = new this();
    // n._batchSize = cursor._batchSize;
    // return n;
  }
  constructor(private collection: Collection, private _filter: any) {

  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#batchSize */
  batchSize(value: number): Cursor<T> {
    if (value < 1) { throw new Error("batchSize must be at least 1"); }
    this._batchSize = value; return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#clone */
  clone(): Cursor<T> {
    let nc = new Cursor<T>(this.collection, this._filter);
    nc.options = this.options;
    nc._batchSize = this._batchSize;
    return nc;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#close */
  async close(): Promise<CursorResult> {
    if (!this.cursorId) { return false; }
    await MongoDBMobile.closeCursor({
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
  collation(value: MongoMobileTypes.Collation): Cursor<T> {
    this.options.collation = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#comment */
  comment(value: string): Cursor<T> {
    this.options.comment = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#count */
  async count(applySkipLimit: boolean = false, options: CursorCommentOptions = {}): Promise<number> {
    let countOpts: MongoMobileTypes.CountOptions = {
      collation: this.options.collation,
      hint: this.options.hint,
      maxTimeMS: this.options.maxTimeMS
    };
    if (options.hint) {
      countOpts.hint = options.hint;
    }
    if (typeof options.maxTimeMS == 'number') {
      countOpts.maxTimeMS = options.maxTimeMS;
    }
    if (applySkipLimit) {
      countOpts.skip = (typeof options.skip == 'number') ? options.skip : this.options.skip;
      countOpts.limit = (typeof options.limit == 'number') ? options.limit : this.options.limit;
    }
    let res = await MongoDBMobile.count({
      db: this.collection.db.databaseName,
      collection: this.collection.collectionName,
      filter: this._filter,
      options: countOpts
    });

    return res.count;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#explain */
  explain(): Promise<CursorResult> {
    throw new Error("Not implemented!");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#filter */
  filter(filter: object): Cursor<T> {
    this._filter = filter;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#forEach */
  async forEach(iterator: IteratorCallback<T>) : Promise<void> {
    let next: T;
    while (next = await this.next()) {
      iterator(next);
    }
    await this.close();
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#hasNext */
  hasNext(): Promise<boolean> {
    // TODO implement
    throw new Error("Not implemented");
  }
  hint(hint: MongoMobileTypes.IndexHint): Cursor<T> {
    this.options.hint = hint;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#isClosed */
  isClosed() {
    return this._isClosed;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#limit */
  limit(value: number): Cursor<T> {
    this.options.limit = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#map */
  map<U>(transform: (document: T) => U): Cursor<U> {
    throw new Error("Not implemented");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#max */
  max(max: object): Cursor<T> {
    this.options.max = max;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#maxAwaitTimeMS */
  maxAwaitTimeMS(value: number): Cursor<T> {
    throw new Error("Not implemented");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#maxScan */
  maxScan(maxScan: number): Cursor<T> {
    this.options.maxScan = maxScan;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#maxTimeMS */
  maxTimeMS(value: number): Cursor<T> {
    this.options.maxTimeMS = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#min */
  min(min: object): Cursor<T> {
    this.options.min = min;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#next */
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

    let next = await MongoDBMobile.cursorGetNext<T>({
      cursorId: this.cursorId, batchSize: this._batchSize
    });
    if (next.complete) {
      this._isEnd;
    }
    this._curBatch = next.results || [];
    
    return this.next();
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#project */
  project(value: object): Cursor<T> {
    this.options.projection = value;
    return this;
  }
  returnKey(returnKey: boolean): Cursor<T> {
    this.options.returnKey = returnKey;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#rewind */
  async rewind() {
    if (!this._isClosed) {
      this.close();
    }
    this._isClosed = false;
    this._isEnd = false;
    this._curBatch = [];
    this.cursorId = null;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#setCursorOption */
  // setCursorOption(field: string, value: object): Cursor<T>;
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#setReadPreference */
  setReadPreference(readPreference: any): Cursor<T> {
    return this; // Total no-op since read preference is meaningless in mongodb mobile
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#showRecordId */
  showRecordId(showRecordId: boolean): Cursor<T> {
    this.options.showRecordId = showRecordId;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#skip */
  skip(value: number): Cursor<T> {
    this.options.skip = value;
    return this;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#snapshot */
  snapshot(snapshot: object): Cursor<T> {
    throw new Error("Not implemented");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#sort */
  sort(keyOrList: MongoMobileTypes.IndexFields): Cursor<T> {
    this.options.sort = keyOrList
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#stream */
  stream(options?: { transform?: (document: T) => any }): Cursor<T> {
    throw new Error("Not implemented");
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/Cursor.html#toArray */
  async toArray(): Promise<T[]> {
    let all: T[] = [];
    let next: T;
    while (next = await this.next()) {
      all.push(next);
    }
    return all;
  }
}