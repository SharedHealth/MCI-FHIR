package org.sharedhealth.mci.web.model;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_MCI_HEALTH_ID;
import static org.sharedhealth.mci.web.util.RepositoryConstants.HID;

@Table(name = CF_MCI_HEALTH_ID)
public class MciHealthId {
    public static MciHealthId NULL_HID = new MciHealthId("00000000000");

    @PartitionKey()
    @Column(name = HID)
    private String hid;

    public MciHealthId(String hid) {
        this.hid = hid;
    }

    public String getHid() {
        return hid;
    }

    public void setHid(String hid) {this.hid = hid;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MciHealthId)) return false;

        MciHealthId MciHealthId = (org.sharedhealth.mci.web.model.MciHealthId) o;

        if (!hid.equals(MciHealthId.hid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hid.hashCode();
    }
}