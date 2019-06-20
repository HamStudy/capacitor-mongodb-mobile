import { MongoMobileTypes } from '../../definitions';
import { Collection } from "../collection";
import { BulkOperation } from './bulkOperation';
import { FindOperatorsOrdered } from './Operation';

export class OrderedBulkOperation extends BulkOperation {
  constructor(collection: Collection, options: MongoMobileTypes.BulkWriteOptions = {}) {
    options.ordered = true;
    super(collection, options);
  }
  
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#find */
  find(selector: object): FindOperatorsOrdered {
    return super.find(selector) as FindOperatorsOrdered;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#insert */
  insert(doc: object): OrderedBulkOperation {
    return super.insert(doc) as OrderedBulkOperation;
  }
}
