package org.sharedhealth.mci.web;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import com.toddfast.mutagen.cassandra.CassandraSubject;
import com.toddfast.mutagen.cassandra.impl.CassandraMutagenImpl;

import java.io.IOException;
import java.util.Map;

public class TestMigrations {
    private static final int ONE_MINUTE = 6000;
    private Map<String, String> env;

    public TestMigrations(Map<String, String> env) {
        this.env = env;
    }

    public void migrate() throws IOException {
        String mciKeyspace = env.get("CASSANDRA_KEYSPACE");
        Cluster cluster = connectKeyspace();
        Session session = createSession(cluster);
        CassandraMutagen mutagen = new CassandraMutagenImpl(mciKeyspace);

        try {
            mutagen.initialize(env.get("CASSANDRA_MIGRATIONS_PATH"));
            com.toddfast.mutagen.Plan.Result<Integer> result = mutagen.mutate(new CassandraSubject(session,
                    mciKeyspace));

            if (result.getException() != null) {
                throw new RuntimeException(result.getException());
            } else if (!result.isMutationComplete()) {
                throw new RuntimeException("Failed to apply cassandra migrations");
            }
        } finally {
            closeConnection(cluster, session);
        }
    }

    private Session createSession(Cluster cluster) {
        String keyspace = env.get("CASSANDRA_KEYSPACE");

        Session session = cluster.connect();
        session.execute(
                String.format(
                        "CREATE KEYSPACE  IF NOT EXISTS %s WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ",
                        keyspace)
        );
        session.close();
        return cluster.connect(keyspace);
    }

    private Cluster connectKeyspace() {
        return connectCluster();
    }

    private Cluster connectCluster() {
        Cluster.Builder clusterBuilder = new Cluster.Builder();

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setConsistencyLevel(ConsistencyLevel.QUORUM);


        PoolingOptions poolingOptions = new PoolingOptions();

        clusterBuilder
                .withPort(Integer.parseInt(env.get("CASSANDRA_PORT")))
                .withClusterName(env.get("CASSANDRA_KEYSPACE"))
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withPoolingOptions(poolingOptions)
                .withProtocolVersion(ProtocolVersion.fromInt(Integer.parseInt(env.get("CASSANDRA_VERSION"))))
                .withQueryOptions(queryOptions)
                .withReconnectionPolicy(new ConstantReconnectionPolicy(ONE_MINUTE))
                .addContactPoint(env.get("CASSANDRA_HOST"));
        return clusterBuilder.build();

    }

    private void closeConnection(Cluster cluster, Session session) {
        session.close();
        cluster.close();
    }


}
