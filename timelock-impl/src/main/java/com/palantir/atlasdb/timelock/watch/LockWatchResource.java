/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.atlasdb.timelock.watch;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.palantir.lock.LockDescriptor;

@Path("/lock-watch")
public class LockWatchResource {
    private static final int WATCH_LIMIT = 50;

    private final Multimap<LockDescriptor, LockWatch> explicitDescriptorsToWatches;
    private final Map<UUID, LockWatch> activeWatches;

    public LockWatchResource() {
        this.explicitDescriptorsToWatches = Multimaps.synchronizedListMultimap(MultimapBuilder.hashKeys()
                .arrayListValues()
                .build());
        this.activeWatches = Maps.newConcurrentMap();
    }

    @PUT
    @Path("watch")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public UUID registerWatch(LockPredicate predicate) {
        if (activeWatches.size() > WATCH_LIMIT) {
            throw new IllegalStateException("Cannot register watch, too many watches registered.");
        }

        UUID idToAssign = UUID.randomUUID();

        if (!(predicate instanceof ExplicitLockPredicate)) {
            throw new IllegalArgumentException("Non-explicit lock predicates not supported yet!");
        }
        ExplicitLockPredicate explicitLockPredicate = (ExplicitLockPredicate) predicate;

        activeWatches.put(idToAssign, new LockWatch(explicitLockPredicate.descriptors()));
        return idToAssign;
    }

    @DELETE
    @Path("watch/{id}")
    void unregisterWatch(@PathParam("id") UUID watchIdentifier) throws NotFoundException {
        LockWatch removed = activeWatches.remove(watchIdentifier);
        if (removed == null) {
            throw new NotFoundException("lock watch does not exist");
        }
    }

    /**
     * @param watchIdentifier watch identifier
     * @return true if and only if some lock guarded by this watch was locked
     * @throws NotFoundException if we do not recognise the watch identifier the user provides
     */
    @POST
    @Path("watch/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    LockWatchState getWatchState(@PathParam("id") UUID watchIdentifier) throws NotFoundException {
        LockWatch watch = activeWatches.get(watchIdentifier);
        if (watch == null) {
            throw new NotFoundException("lock watch does not exist");
        }
        return watch.getState();
    }
}
