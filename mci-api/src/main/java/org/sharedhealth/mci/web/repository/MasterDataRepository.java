package org.sharedhealth.mci.web.repository;


import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.sharedhealth.mci.web.model.MasterData;

public class MasterDataRepository {
    private Mapper<MasterData> masterDataMapper;

    public MasterDataRepository(MappingManager mappingManager) {
        this.masterDataMapper = mappingManager.mapper(MasterData.class);
    }

    public MasterData findByTypeAndKey(String type, String key) {
        return masterDataMapper.get(type, key);
    }
}
