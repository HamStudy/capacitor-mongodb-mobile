package org.hamstudy.MongoDb;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;

import java.util.ArrayList;

public class BulkWriteBatch<TDocument> {
    private MongoCollection<TDocument> collection;
    private BulkWriteOptions opts = null;
    private ArrayList<WriteModel<TDocument>> requests = new ArrayList<>();

    BulkWriteBatch(MongoCollection<TDocument> c, BulkWriteOptions o) {
        this.collection = c;
        this.opts = o;
    }
    void addRequest(WriteModel<TDocument> r) {
        requests.add(r);
    }
    BulkWriteResult execute() {
        return collection.bulkWrite(requests, opts);
    }
}
