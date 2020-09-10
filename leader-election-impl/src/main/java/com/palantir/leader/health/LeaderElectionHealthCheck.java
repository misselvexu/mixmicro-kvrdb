/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.leader.health;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.palantir.leader.LeaderElectionServiceMetrics;
import com.palantir.paxos.Client;

public class LeaderElectionHealthCheck {
    private static final double MAX_ALLOWED_LAST_5_MINUTE_RATE = 0.015;
    private static final Duration HEALTH_CHECK_DEACTIVATION_PERIOD = Duration.ofMinutes(14);
    private final ConcurrentMap<Client, LeaderElectionServiceMetrics> clientWiseMetrics = new ConcurrentHashMap<>();
    private final Instant timeCreated = Instant.now();
    private boolean healthCheckDeactivated = false;

    public void registerClient(Client namespace, LeaderElectionServiceMetrics leaderElectionServiceMetrics) {
        clientWiseMetrics.putIfAbsent(namespace, leaderElectionServiceMetrics);
    }

    private double getLeaderElectionRateForAllClients() {
        return clientWiseMetrics.values().stream().mapToDouble(this::fiveMinuteRate).sum();
    }

    private double fiveMinuteRate(LeaderElectionServiceMetrics leaderElectionRateForClient) {
        return leaderElectionRateForClient.proposedLeadership().getFiveMinuteRate();
    }

    private boolean isHealthCheckDeactivated() {
        if (!healthCheckDeactivated) {
            healthCheckDeactivated
                    = Duration.between(timeCreated, Instant.now()).compareTo(HEALTH_CHECK_DEACTIVATION_PERIOD) < 0;
        }
        return healthCheckDeactivated;
    }

    private boolean isHealthy(double leaderElectionRateForAllClients) {
        return isHealthCheckDeactivated() || (leaderElectionRateForAllClients <= MAX_ALLOWED_LAST_5_MINUTE_RATE);
    }

    public LeaderElectionHealthReport leaderElectionRateHealthReport() {
        double leaderElectionRateForAllClients = getLeaderElectionRateForAllClients();

        return isHealthy(leaderElectionRateForAllClients)
                ? LeaderElectionHealthReport.builder()
                .status(LeaderElectionHealthStatus.HEALTHY)
                .leaderElectionRate(leaderElectionRateForAllClients)
                .build()
                : LeaderElectionHealthReport.builder()
                        .status(LeaderElectionHealthStatus.UNHEALTHY)
                        .leaderElectionRate(leaderElectionRateForAllClients)
                        .build();
    }
}
