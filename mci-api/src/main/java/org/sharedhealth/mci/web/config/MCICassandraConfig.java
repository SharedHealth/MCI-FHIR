package org.sharedhealth.mci.web.config;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

public class MCICassandraConfig {
    private static final MCICassandraConfig mciCassandraConfig = new MCICassandraConfig();
    private Session session;
    private static final int ONE_MINUTE = 6000;

    private MCICassandraConfig() {
        this.session = getOrCreateSession();
    }

    public static MCICassandraConfig getInstance() {
        return mciCassandraConfig;
    }

    public Session getOrCreateSession() {
        if (this.session != null) return this.session;

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

        this.session = clusterBuilder.build().connect(mciProperties.getCassandraKeySpace());
        return this.session;
    }
}
