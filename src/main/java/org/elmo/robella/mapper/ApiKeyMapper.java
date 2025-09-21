package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    List<ApiKey> findByUserId(@Param("userId") Long userId);

    ApiKey findByKeyHash(@Param("keyHash") String keyHash);

    List<ApiKey> findByUserIdAndActive(@Param("userId") Long userId, @Param("active") Boolean active);

    ApiKey findByKeyPrefix(@Param("keyPrefix") String keyPrefix);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}