package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.Model;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelMapper extends BaseMapper<Model> {

    List<Model> findByPublishedTrue();

    Model findByName(@Param("name") String name);

    Model findByModelKey(@Param("modelKey") String modelKey);

    List<Model> findByOrganization(@Param("organization") String organization);

    List<Model> findByOrganizationAndPublishedTrue(@Param("organization") String organization);
}