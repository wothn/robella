package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.VendorModel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VendorModelMapper extends BaseMapper<VendorModel> {

    List<VendorModel> findByModelId(@Param("modelId") Long modelId);

    List<VendorModel> findByProviderId(@Param("providerId") Long providerId);

    List<VendorModel> findByModelIdAndProviderId(@Param("modelId") Long modelId, @Param("providerId") Long providerId);

    List<VendorModel> findByModelIdAndProviderIdAndEnabledTrue(@Param("modelId") Long modelId, @Param("providerId") Long providerId);

    List<VendorModel> findByEnabledTrue();

    VendorModel findByModelIdAndProviderIdAndVendorModelName(@Param("modelId") Long modelId, @Param("providerId") Long providerId, @Param("vendorModelName") String vendorModelName);

    VendorModel findByVendorModelName(@Param("vendorModelName") String vendorModelName);

    VendorModel findByVendorModelKey(@Param("vendorModelKey") String vendorModelKey);

    VendorModel findByVendorModelKeyAndEnabledTrue(@Param("vendorModelKey") String vendorModelKey);

    List<VendorModel> findByModelIdAndEnabledTrue(@Param("modelId") Long modelId);

    List<VendorModel> findByModelKeyAndEnabledTrue(@Param("modelKey") String modelKey);
}