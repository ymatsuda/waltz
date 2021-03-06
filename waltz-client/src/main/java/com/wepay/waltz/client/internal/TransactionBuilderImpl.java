package com.wepay.waltz.client.internal;

import com.wepay.waltz.client.PartitionLocalLock;
import com.wepay.waltz.client.Serializer;
import com.wepay.waltz.client.TransactionBuilder;
import com.wepay.waltz.common.message.AppendRequest;
import com.wepay.waltz.common.message.ReqId;
import com.wepay.waltz.common.util.Utils;

import java.util.List;

/**
 * An implementation of {@link TransactionBuilder} for building {@link AppendRequest}s.
 */
public class TransactionBuilderImpl implements TransactionBuilder {

    private static final int[] NO_LOCKS = new int[0];

    public final ReqId reqId;
    public final long clientHighWaterMark;

    private int header = 0;
    private byte[] data = null;
    private List<PartitionLocalLock> writeLocks;
    private List<PartitionLocalLock> readLocks;
    private List<PartitionLocalLock> appendLocks;

    /**
     * Class Constructor.
     * @param reqId the {@link ReqId} of the transaction request.
     * @param clientHighWaterMark the client high-water mark.
     */
    public TransactionBuilderImpl(ReqId reqId, long clientHighWaterMark) {
        this.reqId = reqId;
        this.clientHighWaterMark = clientHighWaterMark;
    }

    @Override
    public void setHeader(int header) {
        this.header = header;
    }

    @Override
    public <T> void setTransactionData(T transactionData, Serializer<T> serializer) {
        this.data = serializer.serialize(transactionData);
    }

    @Override
    public void setWriteLocks(List<PartitionLocalLock> locks) {
        this.writeLocks = locks;
    }

    @Override
    public void setReadLocks(List<PartitionLocalLock> locks) {
        this.readLocks = locks;
    }

    @Override
    public void setAppendLocks(List<PartitionLocalLock> locks) {
        this.appendLocks = locks;
    }

    /**
     * @return a new instance of {@link AppendRequest} to send to Waltz cluster.
     */
    public AppendRequest buildRequest() {
        return new AppendRequest(
            reqId,
            clientHighWaterMark,
            compileLockRequest(writeLocks),
            compileLockRequest(readLocks),
            compileLockRequest(appendLocks),
            header,
            data,
            Utils.checksum(data)
        );
    }

    private int[] compileLockRequest(List<PartitionLocalLock> partitionLocalLocks) {
        if (partitionLocalLocks == null) {
            return NO_LOCKS;

        } else {
            int[] lockRequest = new int[partitionLocalLocks.size()];

            int i = 0;
            for (PartitionLocalLock partitionLocalLock : partitionLocalLocks) {
                lockRequest[i++] = partitionLocalLock.hashCode();
            }

            return lockRequest;
        }
    }
}

