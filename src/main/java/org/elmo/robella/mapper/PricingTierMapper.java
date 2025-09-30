package org.elmo.robella.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.elmo.robella.model.entity.PricingTier;

import java.util.List;

@Mapper
public interface PricingTierMapper extends BaseMapper<PricingTier> {
    
    /**
     * 根据供应商模型ID获取所有定价阶梯
     * @param vendorModelId 供应商模型ID
     * @return 定价阶梯列表
     */
    List<PricingTier> findByVendorModelId(@Param("vendorModelId") Long vendorModelId);
    
    /**
     * 根据供应商模型ID删除所有定价阶梯
     * @param vendorModelId 供应商模型ID
     * @return 删除的行数
     */
    int deleteByVendorModelId(@Param("vendorModelId") Long vendorModelId);
}