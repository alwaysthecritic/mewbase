package io.mewbase.server.impl;

import io.mewbase.bson.BsonObject;
import io.mewbase.server.Binder;
import io.mewbase.server.DocReadStream;
import io.mewbase.server.QueryContext;
import io.mewbase.server.impl.cqrs.QueryImpl;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tim on 17/11/16.
 */
public abstract class QueryExecution {

    private final static Logger logger = LoggerFactory.getLogger(QueryExecution.class);

    private static final int MAX_UNACKED_BYTES = 4 * 1024 * 1024; // TODO make configurable

    private final DocReadStream readStream;
    private int unackedBytes;
    protected Context context;

    public QueryExecution(QueryImpl query, BsonObject params) {
        Binder binder = query.getBinder();
        readStream = binder.getMatching(doc -> true);
        QueryContext qc = new QueryContext(params);
        readStream.handler(doc -> {
            boolean accepted = query.getDocumentFilter().apply(doc, qc);
            if (accepted) {
                handle(doc, !readStream.hasMore() || qc.isComplete());
            }
        });
    }

    public void start() {
        readStream.start();
    }

    void handleAck(int bytes) {
        checkContext();
        unackedBytes -= bytes;
        // Low watermark to prevent thrashing
        if (unackedBytes < MAX_UNACKED_BYTES / 2) {
            readStream.resume();
        }
    }

    public void handle(BsonObject doc, boolean last) {
        checkContext();
        Buffer buff = writeQueryResult(doc, last);
        unackedBytes += buff.length();
        if (unackedBytes > MAX_UNACKED_BYTES) {
            readStream.pause();
        }
        if (last) {
            close();
        }
    }

    public void close() {
        readStream.close();
    }

    protected abstract Buffer writeQueryResult(BsonObject document, boolean last);

    protected void checkContext() {
        if (context == null) {
            context = Vertx.currentContext();
            if (context == null) {
                throw new IllegalStateException("Not on context!");
            }
        } else if (Vertx.currentContext() != context) {
            logger.trace("Wrong context!! " + Thread.currentThread() + " expected " + context, new Exception());
            throw new IllegalStateException("Wrong context!");
        }
    }
}