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
package com.palantir.atlasdb.memory;

import com.google.auto.service.AutoService;
import com.palantir.atlasdb.cleaner.api.OnCleanupTask;
import com.palantir.atlasdb.config.LeaderConfig;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.impl.InMemoryKeyValueService;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.atlasdb.spi.DerivedSnapshotConfig;
import com.palantir.atlasdb.spi.KeyValueServiceConfig;
import com.palantir.atlasdb.spi.KeyValueServiceRuntimeConfig;
import com.palantir.atlasdb.table.description.Schema;
import com.palantir.atlasdb.util.MetricsManager;
import com.palantir.atlasdb.versions.AtlasDbVersion;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import com.palantir.refreshable.Refreshable;
import com.palantir.timestamp.InMemoryTimestampService;
import com.palantir.timestamp.ManagedTimestampService;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * This is the easiest way to try out AtlasDB with your schema.  It runs entirely in memory but has
 * all the features of a full cluster including {@link OnCleanupTask}s.
 * <p>
 * This method creates all the tables in the pass {@link Schema} and provides Snapshot Isolation
 * (SI) on all of the transactions it creates.
 */
@AutoService(AtlasDbFactory.class)
public class InMemoryAtlasDbFactory implements AtlasDbFactory {
    private static final SafeLogger log = SafeLoggerFactory.get(InMemoryAtlasDbFactory.class);

    @Override
    public String getType() {
        return "memory";
    }

    /**
     * Creates an InMemoryKeyValueService.
     *
     * @param config Configuration file.
     * @param runtimeConfig unused.
     * @param leaderConfig unused.
     * @param unused unused.
     * @param unusedLongSupplier unused.
     * @param initializeAsync unused. Async initialization has not been implemented and is not propagated.
     * @return The requested KeyValueService instance
     */
    @Override
    public InMemoryKeyValueService createRawKeyValueService(
            MetricsManager metricsManager,
            KeyValueServiceConfig config,
            Refreshable<Optional<KeyValueServiceRuntimeConfig>> runtimeConfig,
            Optional<LeaderConfig> leaderConfig,
            Optional<String> unused,
            LongSupplier unusedLongSupplier,
            boolean initializeAsync) {
        if (initializeAsync) {
            log.warn("Asynchronous initialization not implemented, will initialize synchronously.");
        }

        AtlasDbVersion.ensureVersionReported();
        return new InMemoryKeyValueService(false);
    }

    @Override
    public DerivedSnapshotConfig createDerivedSnapshotConfig(
            KeyValueServiceConfig config, Optional<KeyValueServiceRuntimeConfig> runtimeConfigSnapshot) {
        InMemoryAtlasDbConfig inMemoryAtlasDbConfig = (InMemoryAtlasDbConfig) config;
        return DerivedSnapshotConfig.builder()
                .concurrentGetRangesThreadPoolSize(inMemoryAtlasDbConfig.concurrentGetRangesThreadPoolSize())
                .defaultGetRangesConcurrencyOverride(inMemoryAtlasDbConfig.defaultGetRangesConcurrency())
                .build();
    }

    @Override
    public ManagedTimestampService createManagedTimestampService(
            KeyValueService rawKvs, Optional<TableReference> unused, boolean initializeAsync) {
        if (initializeAsync) {
            log.warn("Asynchronous initialization not implemented, will initialize synchronously.");
        }

        AtlasDbVersion.ensureVersionReported();
        return new InMemoryTimestampService();
    }
}
