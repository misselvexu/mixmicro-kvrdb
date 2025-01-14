/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.workload.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.palantir.atlasdb.protos.generated.TableMetadataPersistence;
import com.palantir.atlasdb.transaction.api.ConflictHandler;
import com.palantir.atlasdb.workload.store.IsolationLevel;
import org.junit.Test;

public class AtlasDbUtilsTest {
    @Test
    public void indexMetadataUsesTheCorrectConflictHandler() throws InvalidProtocolBufferException {
        assertThat(TableMetadataPersistence.TableMetadata.parseFrom(
                                AtlasDbUtils.indexMetadata(ConflictHandler.SERIALIZABLE))
                        .getConflictHandler())
                .isEqualTo(TableMetadataPersistence.TableConflictHandler.SERIALIZABLE_INDEX);
        assertThat(TableMetadataPersistence.TableMetadata.parseFrom(
                                AtlasDbUtils.indexMetadata(ConflictHandler.RETRY_ON_WRITE_WRITE))
                        .getConflictHandler())
                .isEqualTo(TableMetadataPersistence.TableConflictHandler.IGNORE_ALL);
        assertThat(TableMetadataPersistence.TableMetadata.parseFrom(
                                AtlasDbUtils.indexMetadata(ConflictHandler.IGNORE_ALL))
                        .getConflictHandler())
                .isEqualTo(TableMetadataPersistence.TableConflictHandler.IGNORE_ALL);
        assertThat(TableMetadataPersistence.TableMetadata.parseFrom(
                                AtlasDbUtils.indexMetadata(ConflictHandler.RETRY_ON_VALUE_CHANGED))
                        .getConflictHandler())
                .isEqualTo(TableMetadataPersistence.TableConflictHandler.IGNORE_ALL);
    }

    @Test
    public void toConflictHandlerHandlesAllIsolationLevelCases() {
        assertThat(AtlasDbUtils.toConflictHandler(IsolationLevel.NONE)).isEqualTo(ConflictHandler.IGNORE_ALL);
        assertThat(AtlasDbUtils.toConflictHandler(IsolationLevel.SNAPSHOT))
                .isEqualTo(ConflictHandler.RETRY_ON_WRITE_WRITE);
        assertThat(AtlasDbUtils.toConflictHandler(IsolationLevel.SERIALIZABLE)).isEqualTo(ConflictHandler.SERIALIZABLE);
    }
}
