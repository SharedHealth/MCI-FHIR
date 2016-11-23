package org.sharedhealth.mci.web.model;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_MASTER_DATA)
public class MasterData {
    @PartitionKey
    @Column(name = TYPE)
    private String type;

    @PartitionKey(value = 1)
    @Column(name = KEY)
    private String key;

    @Column(name = VALUE)
    private String value;

    public MasterData() {
    }

    public MasterData(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
