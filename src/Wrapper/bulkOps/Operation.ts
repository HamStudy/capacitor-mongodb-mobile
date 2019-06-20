
import {BulkOperation, addOperation, OperationTypes} from './bulkOperation';
import { OrderedBulkOperation } from './OrderedBulkOperation';
import {UnorderedBulkOperation} from './UnorderedBulkOperation';

export interface FindOperatorsOrdered {
    delete(): OrderedBulkOperation;
    deleteOne(): OrderedBulkOperation;
    replaceOne(doc: object): OrderedBulkOperation;
    update(doc: object): OrderedBulkOperation;
    updateOne(doc: object): OrderedBulkOperation;
    upsert(): FindOperatorsOrdered;
}
export interface FindOperatorsUnordered {
    remove(): UnorderedBulkOperation;
    removeOne(): UnorderedBulkOperation;
    replaceOne(doc: object): UnorderedBulkOperation;
    update(doc: object): UnorderedBulkOperation;
    updateOne(doc: object): UnorderedBulkOperation;
    upsert(): FindOperatorsUnordered;
}
export class Operation implements FindOperatorsOrdered, FindOperatorsUnordered {
  private _upsert: boolean = false;
  
  constructor(
    private readonly parent: BulkOperation,
    private readonly addOp: addOperation,
    private selector: any
    ) {
  }

  remove(): any {
    this.addOp({operation: OperationTypes.DeleteMany, selector: this.selector })
    return this.parent;
  }
  delete(): any {
    return this.remove();
  }
  removeOne(): any {
    this.addOp({operation: OperationTypes.DeleteOne, selector: this.selector});
    return this.parent;
  }
  deleteOne(): any {
    return this.removeOne();
  }
  replaceOne(doc: object): any {
    this.addOp({operation: OperationTypes.ReplaceOne, doc: doc, selector: this.selector, upsert: this._upsert});
    return this.parent as OrderedBulkOperation;
  }
  update(doc: object): any {
    this.addOp({operation: OperationTypes.UpdateMany, doc: doc, selector: this.selector, upsert: this._upsert});
    return this.parent as OrderedBulkOperation;
  }
  updateOne(doc: object): any {
    this.addOp({operation: OperationTypes.UpdateOne, doc: doc, selector: this.selector, upsert: this._upsert});
    return this.parent as OrderedBulkOperation;
  }
  upsert() : any {
    this._upsert = true;
    return this;
  }

}
