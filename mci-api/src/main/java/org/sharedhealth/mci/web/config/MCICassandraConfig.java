package org.sharedhealth.mci.web.config;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.mapping.MappingManager;

public class MCICassandraConfig {
    private static final MCICassandraConfig mciCassandraConfig = new MCICassandraConfig();
    private static MappingManager mappingManager;
    private static final int ONE_MINUTE = 6000;

    private MCICassandraConfig() {
        mappingManager = new MappingManager(getOrCreateSession());
    }

    public static MCICassandraConfig getInstance() {
        return mciCassandraConfig;
    }

    public MappingManager getMappingManager() {
        return mappingManager;
    }

    private Session getOrCreateSession() {
        MCIProperties mciProperties = MCIProperties.getInstance();
        Cluster.Builder clusterBuilder = new Cluster.Builder();

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setConsistencyLevel(ConsistencyLevel.QUORUM);

        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setConnectTimeoutMillis(mciProperties.getCassandraTimeout());
        socketOptions.setReadTimeoutMillis(mciProperties.getCassandraTimeout());

        clusterBuilder
                .withPort(mciProperties.getCassandraPort())
                .withClusterName(mciProperties.getCassandraKeySpace())
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withPoolingOptions(new PoolingOptions())
                .withAuthProvider(new PlainTextAuthProvider(mciProperties.getCassandraUser(), mciProperties.getCassandraPassword()))
                .withProtocolVersion(ProtocolVersion.fromInt(mciProperties.getCassandraVersion()))
                .withQueryOptions(queryOptions)
                .withSocketOptions(socketOptions)
                .withReconnectionPolicy(new ConstantReconnectionPolicy(ONE_MINUTE))
                .addContactPoint(mciProperties.getCassandraHost());

        return clusterBuilder.build().connect(mciProperties.getCassandraKeySpace());
    }
}
