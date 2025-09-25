package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.Model;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelMapper extends BaseMapper<Model> {
    
    List<Model> findByPublishedTrue();
    
    List<Model> searchModels(@Param("keyword") String keyword);
    
    List<Model> findByOrganization(@Param("organization") String organization);
    
    List<Model> findByCapability(@Param("capability") String capability);
    
    List<Model> findByModelId(@Param("modelId") Long modelId);
}