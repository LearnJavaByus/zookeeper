/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.quorum;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.jute.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.FinalRequestProcessor;
import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.RequestProcessor;
import org.apache.zookeeper.server.SyncRequestProcessor;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.txn.TxnHeader;

/**
 * Just like the standard ZooKeeperServer. We just replace the request
 * processors: FollowerRequestProcessor -> CommitProcessor ->
 * FinalRequestProcessor
 *
 * A SyncRequestProcessor is also spawned off to log proposals from the leader.
 *
 * Follower服务器，继承了LearnerZooKeeper，其请求处理链为FollowerRequestProcessor -> CommitProcessor -> FinalRequestProcessor。
 *
 * 其核心是对待同步请求和待处理事务请求交由不同的请求处理器进行处理
 */
public class FollowerZooKeeperServer extends LearnerZooKeeperServer {
    private static final Logger LOG =
        LoggerFactory.getLogger(FollowerZooKeeperServer.class);

    /*
     * Pending sync requests // 待同步请求
     */
    ConcurrentLinkedQueue<Request> pendingSyncs;

    /**
     * @param port
     * @param dataDir
     * @throws IOException
     */
    FollowerZooKeeperServer(FileTxnSnapLog logFactory,QuorumPeer self,
            ZKDatabase zkDb) throws IOException {
        super(logFactory, self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, zkDb, self);
        this.pendingSyncs = new ConcurrentLinkedQueue<Request>(); // 初始化pendingSyncs
    }

    public Follower getFollower(){
        return self.follower;
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        commitProcessor = new CommitProcessor(finalProcessor,
                Long.toString(getServerId()), true, getZooKeeperServerListener());
        commitProcessor.start();
        firstProcessor = new FollowerRequestProcessor(this, commitProcessor);
        ((FollowerRequestProcessor) firstProcessor).start();
        syncProcessor = new SyncRequestProcessor(this,
                new SendAckRequestProcessor((Learner)getFollower()));
        syncProcessor.start();
    }
    // // 待处理的事务请求
    LinkedBlockingQueue<Request> pendingTxns = new LinkedBlockingQueue<Request>();

    public void logRequest(TxnHeader hdr, Record txn) {
        // 创建请求
        Request request = new Request(hdr.getClientId(), hdr.getCxid(), hdr.getType(), hdr, txn, hdr.getZxid());
        if ((request.zxid & 0xffffffffL) != 0) {
            pendingTxns.add(request);
        }
        // 使用SyncRequestProcessor处理请求(其会将请求放在队列中，异步进行处理)
        syncProcessor.processRequest(request);
    }

    /**
     * When a COMMIT message is received, eventually this method is called,
     * which matches up the zxid from the COMMIT with (hopefully) the head of
     * the pendingTxns queue and hands it to the commitProcessor to commit.
     * @param zxid - must correspond to the head of pendingTxns if it exists
     *
     *  提交zxid对应的请求（pendingTxns的队首元素），其首先会判断队首请求对应的zxid是否为传入的zxid，然后再进行移除和提交（放在committedRequests队列中）。
     */
    public void commit(long zxid) {
        if (pendingTxns.size() == 0) { // 没有还在等待处理的事务
            LOG.warn("Committing " + Long.toHexString(zxid)
                    + " without seeing txn");
            return;
        }
        // 队首元素的zxid
        long firstElementZxid = pendingTxns.element().zxid;
        if (firstElementZxid != zxid) { // 如果队首元素的zxid不等于需要提交的zxid，则退出程序
            LOG.error("Committing zxid 0x" + Long.toHexString(zxid)
                    + " but next pending txn 0x"
                    + Long.toHexString(firstElementZxid));
            System.exit(12);
        }
        // 从待处理事务请求队列中移除队首请求
        Request request = pendingTxns.remove();
        // 提交该请求
        commitProcessor.commit(request);
    }

    synchronized public void sync(){
        if(pendingSyncs.size() ==0){ // 没有需要同步的请求
            LOG.warn("Not expecting a sync.");
            return;
        }
        // 从待同步队列中移除队首请求
        Request r = pendingSyncs.remove();
		commitProcessor.commit(r); // 提交该请求
    }

    @Override
    public int getGlobalOutstandingLimit() {
        int divisor = self.getQuorumSize() > 2 ? self.getQuorumSize() - 1 : 1;
        return super.getGlobalOutstandingLimit() / divisor;
    }

    @Override
    public String getState() {
        return "follower";
    }

    @Override
    public Learner getLearner() {
        return getFollower();
    }
}
