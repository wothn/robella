package org.elmo.robella.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.elmo.robella.model.entity.PricingTier;


@Mapper
public interface PricingTierMapper extends BaseMapper<PricingTier> {
    
}