import { Plugins } from '@capacitor/core';
import { MongoDBMobilePlugin, MongoMobileTypes } from '../../definitions';
const MongoDBMobile = Plugins.MongoDBMobile as MongoDBMobilePlugin;

import { Collection } from "../collection";
import { FindOperatorsOrdered, FindOperatorsUnordered, Operation } from './Operation';

export interface BulkWriteResult {
    ok: number;
    nInserted: number;
    nUpdated: number;
    nUpserted: number;
    nModified: number;
    nRemoved: number;
    insertedIds?: any[];
    upsertedIds?: any[];
}

export enum OperationTypes {
  DeleteOne,
  DeleteMany,
  InsertOne,
  ReplaceOne,
  UpdateOne,
  UpdateMany
};
export interface OperationRecord {
  operation: OperationTypes;
  selector?: any;
  doc?: any,
  upsert?: boolean
};
export type addOperation = (op: OperationRecord) => void;

function invalidOperation(type: never) {
  return new Error("Invalid operation:" + type);
}

export class BulkOperation {
  private doneInit: boolean = false;
  private initOp: Promise<any>;
  private operationId: string;

  private pendingOps: Promise<any>[] = [];

  enqueuePromise(op: Promise<any>) {
    this.pendingOps.push(op);
    op.catch(() => void 0).then(() => {
      let idx = this.pendingOps.indexOf(op);
      if (idx > -1) {
        this.pendingOps.splice(idx, 1);
      }
    });
  }
  private runOp(op: OperationRecord) : Promise<any> {
    switch(op.operation) {
      case OperationTypes.DeleteMany:
        return MongoDBMobile.bulkWriteAddDeleteMany({
          operationId: this.operationId,
          filter: op.selector,
          options: null // TODO: find a way to implement collation
        });
      case OperationTypes.DeleteOne:
          return MongoDBMobile.bulkWriteAddDeleteOne({
            operationId: this.operationId,
            filter: op.selector,
            options: null, // TODO: find a way to implement collation
          });
      case OperationTypes.InsertOne:
          return MongoDBMobile.bulkWriteAddInsertOne({
            operationId: this.operationId,
            doc: op.doc,
          });
      case OperationTypes.ReplaceOne:
          return MongoDBMobile.bulkWriteAddReplaceOne({
            operationId: this.operationId,
            filter: op.selector,
            replacement: op.doc,
            options: {upsert: !!op.upsert} // TODO: find a way to implement collation
          });
      case OperationTypes.UpdateMany:
          return MongoDBMobile.bulkWriteAddUpdateMany({
            operationId: this.operationId,
            filter: op.selector,
            update: op.doc,
            options: {upsert: !!op.upsert} // TODO: find a way to implement collation
          });
      case OperationTypes.UpdateOne:
          return MongoDBMobile.bulkWriteAddUpdateOne({
            operationId: this.operationId,
            filter: op.selector,
            update: op.doc,
            options: {upsert: !!op.upsert} // TODO: find a way to implement collation
          });
      default:
        throw invalidOperation(op.operation);
    }
  }
  addOp(op: OperationRecord) {
    let empty = new Promise<any>(res => res());
    if (!this.initOp) {
      this.doInit();
    }
    if (!this.operationId || this.options.ordered) {
      let opPromise = Promise.all(this.pendingOps).then(() => {
        return this.runOp(op);
      });
      this.enqueuePromise(opPromise);
    } else {
      let opPromise = this.runOp(op);
      this.enqueuePromise(opPromise);
    }
  }

  constructor(private readonly collection: Collection, private readonly options: MongoMobileTypes.BulkWriteOptions = {}) {
    // By default always ensure successful write
    options.writeConcern = options.writeConcern || {
      w: 1, j: true
    };
  }

  private doInit() {
    let initOp = this.initOp = MongoDBMobile.newBulkWrite({
      db: this.collection.db.databaseName,
      collection: this.collection.collectionName,
      options: this.options
    });
    initOp.then(res => {
      this.operationId = res.operationId;
    });
    this.enqueuePromise(initOp);
  }

  async execute(): Promise<BulkWriteResult> {
    await Promise.all(this.pendingOps);

    let res = await MongoDBMobile.bulkWriteExecute({
      operationId: this.operationId
    });

    return {
      nUpserted: res.upsertedCount,
      nInserted: res.insertedCount,
      nModified: res.modifiedCount,
      nRemoved: res.deletedCount,
      nUpdated: res.modifiedCount, // TODO: is this the correct way to define this result?
      insertedIds: res.insertedIds,
      upsertedIds: res.upsertedIds,
      ok: res.matchedCount,
    };
  }

  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#find */
  find(selector: object): FindOperatorsOrdered | FindOperatorsUnordered {
    return new Operation(this, this.addOp.bind(this), {selector});
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#insert */
  insert(doc: object): BulkOperation {
    this.addOp({operation: OperationTypes.InsertOne, doc: doc});
    return this;
  }
}

