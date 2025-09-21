package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.Provider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProviderMapper extends BaseMapper<Provider> {

    List<Provider> findByEnabledTrue();

    Provider findByName(@Param("name") String name);

    Provider findByEnabledTrueAndName(@Param("name") String name);

    List<Provider> findByEndpointType(@Param("endpointType") String type);
}