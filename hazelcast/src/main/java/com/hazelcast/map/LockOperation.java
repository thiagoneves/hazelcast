/*
 * Copyright (c) 2008-2012, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map;

import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.BackupAwareOperation;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.ResponseHandler;

public class LockOperation extends LockAwareOperation implements BackupAwareOperation {

    public static final long DEFAULT_LOCK_TTL = 5 * 60 * 1000;
    PartitionContainer pc;
    ResponseHandler responseHandler;
    RecordStore recordStore;
    MapService mapService;
    NodeEngine nodeEngine;
    boolean locked = false;

    public LockOperation(String name, Data dataKey) {
        this(name, dataKey, DEFAULT_LOCK_TTL);
    }

    public LockOperation(String name, Data dataKey, long ttl) {
        super(name, dataKey);
        this.ttl = ttl;
    }

    public LockOperation() {
    }

    protected void init() {
        responseHandler = getResponseHandler();
        mapService = getService();
        nodeEngine = getNodeEngine();
        pc = mapService.getPartitionContainer(getPartitionId());
        recordStore = pc.getRecordStore(name);
    }

    public void beforeRun() {
        init();
    }

    public void run() {
        locked = recordStore.lock(getKey(), getCaller(), threadId, ttl);
    }

    public boolean shouldBackup() {
        return locked;
    }

    @Override
    public Object getResponse() {
        return locked;
    }

    @Override
    public void onWaitExpire() {
        getResponseHandler().sendResponse(false);
    }


    public int getSyncBackupCount() {
        return mapService.getMapInfo(name).getBackupCount();
    }

    public int getAsyncBackupCount() {
        return mapService.getMapInfo(name).getAsyncBackupCount();
    }

    public Operation getBackupOperation() {
        return new LockBackupOperation(name, dataKey, null, ttl);
    }
}
