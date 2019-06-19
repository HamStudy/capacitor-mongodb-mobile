import { MongoMobileTypes } from '../../definitions';
import { Collection } from "../collection";
import { BulkOperation } from './bulkOperation';
import { FindOperatorsUnordered } from './Operation';

export class UnorderedBulkOperation extends BulkOperation {
  constructor(collection: Collection, options: MongoMobileTypes.BulkWriteOptions = {}) {
    options.ordered = false;
    super(collection, options);
  }

  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#find */
  find(selector: object): FindOperatorsUnordered {
    return super.find(selector) as FindOperatorsUnordered;
  }
  /** http://mongodb.github.io/node-mongodb-native/3.1/api/OrderedBulkOperation.html#insert */
  insert(doc: object): UnorderedBulkOperation {
    return super.insert(doc) as UnorderedBulkOperation;
  }
}
