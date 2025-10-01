package org.elmo.robella.controller;

import org.elmo.robella.aspect.RoleAspect;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.dto.VendorModelDTO;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.common.ProviderType;
import org.elmo.robella.service.VendorModelService;
import org.elmo.robella.service.ApiKeyService;
import org.apache.ibatis.session.SqlSessionFactory;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VendorModelController.class)
@Import(RoleAspect.class)
@ImportAutoConfiguration(exclude = {
    MybatisPlusAutoConfiguration.class,
    MybatisPlusLanguageDriverAutoConfiguration.class
})
class VendorModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendorModelService vendorModelService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private SqlSessionFactory sqlSessionFactory;

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void shouldReturnVendorModelsByProviderId() throws Exception {
        VendorModelDTO dto = new VendorModelDTO();
        dto.setId(1L);
        dto.setModelId(10L);
        dto.setModelKey("test-model");
        dto.setProviderId(2L);
        dto.setProviderType(ProviderType.OPENAI);
        dto.setVendorModelName("Test Vendor Model");
        dto.setVendorModelKey("test-vendor-model");
        dto.setCachedInputPrice(BigDecimal.ZERO);
        dto.setPricingStrategy(PricingStrategyType.FIXED);
        dto.setWeight(BigDecimal.ONE);
        dto.setEnabled(true);

        given(vendorModelService.getVendorModelsByProviderId(2L)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/vendor-models/provider/{providerId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vendorModelKey").value("test-vendor-model"));
    }

    @Test
    void shouldCreateVendorModel() throws Exception {
        RequestContextHolder.setContext(RequestContextHolder.RequestContext.builder()
                .role(Role.ROOT.name())
                .build());

        VendorModel vendorModel = new VendorModel();
        vendorModel.setModelId(10L);
        vendorModel.setModelKey("test-model");
        vendorModel.setProviderId(2L);
        vendorModel.setProviderType(ProviderType.OPENAI);
        vendorModel.setVendorModelName("New Vendor Model");
        vendorModel.setVendorModelKey("new-vendor-model");
        vendorModel.setCachedInputPrice(BigDecimal.ZERO);
        vendorModel.setPricingStrategy(PricingStrategyType.FIXED);
        vendorModel.setWeight(BigDecimal.valueOf(1.0));
        vendorModel.setEnabled(true);

        VendorModelDTO.CreateRequest request = new VendorModelDTO.CreateRequest();
        request.setVendorModel(vendorModel);

        VendorModelDTO created = new VendorModelDTO();
        created.setId(5L);
        created.setModelId(10L);
        created.setModelKey("test-model");
        created.setProviderId(2L);
        created.setProviderType(ProviderType.OPENAI);
        created.setVendorModelName("New Vendor Model");
        created.setVendorModelKey("new-vendor-model");
        created.setCachedInputPrice(BigDecimal.ZERO);
        created.setPricingStrategy(PricingStrategyType.FIXED);
        created.setWeight(BigDecimal.valueOf(1.0));
        created.setEnabled(true);

        given(vendorModelService.createVendorModel(ArgumentMatchers.any(VendorModelDTO.CreateRequest.class)))
                .willReturn(created);

        mockMvc.perform(post("/api/vendor-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorModelKey").value("new-vendor-model"));
    }
}
