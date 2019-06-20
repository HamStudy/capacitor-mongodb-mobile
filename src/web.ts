import { WebPlugin } from '@capacitor/core';
import { MongoDBMobilePlugin, MongoMobileTypes } from './definitions';

export class MongoDBMobileWeb extends WebPlugin implements MongoDBMobilePlugin {
  createIndexes(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.CreateIndexOptions; indexes: [any, MongoMobileTypes.IndexOptions][]; }): Promise<{ indexesCreated: string[]; }> {
    throw new Error("Method not implemented.");
  }
  dropIndex(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DropIndexOptions,
    name: string,
  }) : Promise<any>;
  dropIndex(options: MongoMobileTypes.DatabaseDef & {
    options?: MongoMobileTypes.DropIndexOptions,
    keys: object,
  }) : Promise<any>;
  dropIndex(options: any) : Promise<any> {
    throw new Error("Method not implemented.");
  }
  listIndexes(options: MongoMobileTypes.DatabaseDef): Promise<{ results: any[]; }> {
    throw new Error("Method not implemented.");
  }
  createCollection(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.CollectionCreateOptions; }): Promise<{ collection: string; }> {
    throw new Error("Method not implemented.");
  }
  runCommand(options: { db: string; command: any; options?: { writeConcern: number | MongoMobileTypes.WriteConcern; }; }): Promise<{ reply: any; }> {
    throw new Error("Method not implemented.");
  }
  dropDatabase(options: { db: string; }): Promise<{ dropped: boolean; }> {
    throw new Error("Method not implemented.");
  }
  dropCollection(options: { db: string; collection: string; }): Promise<{ dropped: boolean; }> {
    throw new Error("Method not implemented.");
  }
  listCollections(options: { db: string; }): Promise<any[]> {
    throw new Error("Method not implemented.");
  }
  initWithId(options: { appID: string; }): Promise<void> {
    throw new Error("Method not implemented.");
  }
  listDatabases(): Promise<any[]> {
    throw new Error("Method not implemented.");
  }
  count(options: MongoMobileTypes.DatabaseDef & { filter: any; options?: MongoMobileTypes.CountOptions; }): Promise<{ count: number; }> {
    throw new Error("Method not implemented.");
  }
  find<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & { cursor?: false; filter: any; options?: MongoMobileTypes.FindOptions; }): Promise<{ results: T[]; }>;
  find(options: MongoMobileTypes.DatabaseDef & { cursor: true; filter: any; options?: MongoMobileTypes.FindOptions; }): Promise<{ cursorId: string; }>;
  find(options: any) : Promise<any> {
    throw new Error("Method not implemented.");
  }
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
  aggregate(options: any) : Promise<any> {
    throw new Error("Method not implemented.");
  }
  cursorGetNext<T extends MongoMobileTypes.Document>(options: { cursorId: string; batchSize?: number; }): Promise<{ results: T[]; complete?: true; }> {
    throw new Error("Method not implemented.");
  }
  closeCursor(options: { cursorId: string; }): Promise<{ success: true; removed: boolean; }> {
    throw new Error("Method not implemented.");
  }
  insertOne<T extends object>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.InsertOneOptions; doc: T; }): Promise<{ success: true; insertedId?: string; }> {
    throw new Error("Method not implemented.");
  }
  insertMany<T extends object>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.InsertManyOptions; docs: T[]; }): Promise<{ success: true; insertedIds?: string[]; insertedCount: number; }> {
    throw new Error("Method not implemented.");
  }
  replaceOne<T extends object>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.ReplaceOptions; filter: any; doc: T; }): Promise<{ success: true; upsertedId?: string; matchedCount: number; modifiedCount: number; upsertedCount: number; }> {
    throw new Error("Method not implemented.");
  }
  updateOne(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.UpdateOptions; filter: any; update: any; }): Promise<MongoMobileTypes.UpdateResult> {
    throw new Error("Method not implemented.");
  }
  updateMany(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.UpdateOptions; filter: any; update: any; }): Promise<MongoMobileTypes.UpdateResult> {
    throw new Error("Method not implemented.");
  }
  deleteOne(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.DeleteOptions; filter: any; }): Promise<{ success: true; deletedCount?: number; }> {
    throw new Error("Method not implemented.");
  }
  deleteMany(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.DeleteOptions; filter: any; }): Promise<{ success: true; deletedCount?: number; }> {
    throw new Error("Method not implemented.");
  }
  findOneAndDelete<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.FindOneAndDeleteOptions; filter: any; }): Promise<{ doc: T; }> {
    throw new Error("Method not implemented.");
  }
  findOneAndReplace<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.FindOneAndReplaceOptions; filter: any; replacement: T; }): Promise<{ doc: T; }> {
    throw new Error("Method not implemented.");
  }
  findOneAndUpdate<T extends MongoMobileTypes.Document>(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.FindOneAndUpdateOptions; filter: any; update: any; }): Promise<{ doc: T; }> {
    throw new Error("Method not implemented.");
  }
  newBulkWrite(options: MongoMobileTypes.DatabaseDef & { options?: MongoMobileTypes.FindOneAndUpdateOptions; }): Promise<{ operationId: string; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddDeleteOne(options: { operationId: string; filter: any; options?: MongoMobileTypes.DeleteModelOptions; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddDeleteMany(options: { operationId: string; filter: any; options?: MongoMobileTypes.DeleteModelOptions; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddInsertOne<T extends object>(options: { operationId: string; doc: T; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddReplaceOne<T extends object>(options: { operationId: string; filter: any; replacement: T; options?: MongoMobileTypes.ReplaceOneModelOptions; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddUpdateOne(options: { operationId: string; filter: any; update: any; options?: MongoMobileTypes.UpdateModelOptions; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteAddUpdateMany(options: { operationId: string; filter: any; update: any; options?: MongoMobileTypes.UpdateModelOptions; }): Promise<{ success: true; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteCancel(options: { operationId: string; }): Promise<{ removed: boolean; }> {
    throw new Error("Method not implemented.");
  }
  bulkWriteExecute(options: { operationId: string; }): Promise<MongoMobileTypes.BulkWriteResult> {
    throw new Error("Method not implemented.");
  }
  constructor() {
    super({
      name: 'MongoDBMobile',
      platforms: ['web', 'ios']
    });
  }
}

const MongoDBMobile = new MongoDBMobileWeb();

export { MongoDBMobile };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(MongoDBMobile);
