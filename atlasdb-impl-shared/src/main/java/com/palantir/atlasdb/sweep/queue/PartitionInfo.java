/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.atlasdb.sweep.queue;

import com.palantir.atlasdb.table.description.SweeperStrategy;
import org.immutables.value.Value;

@Value.Immutable
public interface PartitionInfo {
    ShardAndStrategy shardAndStrategy();

    long timestamp();

    static PartitionInfo of(int shard, SweeperStrategy strategy, long timestamp) {
        return ImmutablePartitionInfo.builder()
                .shardAndStrategy(ShardAndStrategy.of(shard, strategy))
                .timestamp(timestamp)
                .build();
    }
}
