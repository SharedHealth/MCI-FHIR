package org.sharedhealth.mci.web.util;

import com.datastax.driver.core.Session;
import org.sharedhealth.mci.web.config.MCICassandraConfig;

import java.util.List;

import static java.util.Arrays.asList;
import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_MCI_HEALTH_ID;
import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_ORG_HEALTH_ID;
import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_PATIENT;

public class TestUtil {

    public static void truncateAllColumnFamilies() {
        Session session = MCICassandraConfig.getInstance().getMappingManager().getSession();
        List<String> cfs = getAllColumnFamilies();
        for (String cf : cfs) {
            session.execute("truncate " + cf);
        }
    }

    private static List<String> getAllColumnFamilies() {
        return asList(
                CF_PATIENT,
                CF_MCI_HEALTH_ID,
                CF_ORG_HEALTH_ID
        );
    }

}
