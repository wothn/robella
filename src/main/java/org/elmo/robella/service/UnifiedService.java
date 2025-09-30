package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.elmo.robella.exception.BusinessException;
import org.elmo.robella.exception.InsufficientCreditsException;
import org.elmo.robella.mapper.ModelMapper;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.util.BillingUtils;
import org.elmo.robella.util.TokenCountingUtils;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedService {

    private final RoutingService routingService;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final BillingUtils billingUtils;
    private final TokenCountingUtils tokenCountingUtils;

    // 预扣费系数：基于最大可能的输出token比例进行预扣费
    private static final BigDecimal PRE_BILLING_OUTPUT_RATIO = BigDecimal.valueOf(4.0);
    // 最小预扣费金额（CNY）
    private static final BigDecimal MIN_PRE_BILLING_AMOUNT = BigDecimal.valueOf(0.01);

    public ModelListResponse listModels() {
        List<Model> models = modelMapper.findByPublishedTrue();
        
        // 添加空值检查
        if (models == null) {
            models = List.of();
        }
        
        List<ModelInfo> modelInfos = models.stream()
            .map(this::convertToModelInfo)
            .toList();

        ModelListResponse response = new ModelListResponse();
        response.setObject("list");
        response.setData(modelInfos);
        return response;
    }
    
    /**
     * 将Model实体转换为ModelInfo对象
     * @param model Model实体
     * @return ModelInfo对象
     */
    private ModelInfo convertToModelInfo(Model model) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setId(model.getModelKey());
        modelInfo.setObject("model");
        modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
        return modelInfo;
    }

    public UnifiedChatResponse sendChatRequest(UnifiedChatRequest request) {
        RoutingService.ClientWithInfo clientWithInfo = beforeRequest(request);
        return clientWithInfo.getClient().chat(request, clientWithInfo.getProvider());
    }

    public Stream<UnifiedStreamChunk> sendStreamRequest(UnifiedChatRequest request) {
        RoutingService.ClientWithInfo clientWithInfo = beforeRequest(request);
        return clientWithInfo.getClient().chatStream(request, clientWithInfo.getProvider());
    }

    /**
     * 估算请求token数量并计算预估成本
     * 使用TokenCountingUtils进行专业的token计算
     * @param request 统一聊天请求
     * @param vendorModel 供应商模型配置
     * @return 预估成本（CNY）
     */
    private BigDecimal estimateRequestCost(UnifiedChatRequest request, VendorModel vendorModel) {
        try {
            // 如果是按次计费，直接返回固定价格
            if (vendorModel.getPricingStrategy() == PricingStrategyType.PER_REQUEST) {
                // 使用inputPerMillionTokens作为每次请求的固定价格
                return vendorModel.getInputPerMillionTokens()
                    .setScale(6, RoundingMode.HALF_UP)
                    .max(MIN_PRE_BILLING_AMOUNT);
            }
            
            // 使用TokenCountingUtils专业方法估算输入token数量
            int estimatedInputTokens = tokenCountingUtils.estimateRequestTokens(request, vendorModel.getModelKey());
            
            // 估算输出 token 数量，300 token
            int estimatedOutputTokens = 300; 
            
            // 创建临时的Usage对象用于成本计算
            Usage estimatedUsage = new Usage();
            estimatedUsage.setPromptTokens(estimatedInputTokens);
            estimatedUsage.setCompletionTokens(estimatedOutputTokens);
            estimatedUsage.setTotalTokens(estimatedInputTokens + estimatedOutputTokens);
            
            // 计算预估成本
            BillingUtils.BillingResult estimatedCost = billingUtils.calculateCost(estimatedUsage);
            
            // 应用预扣费系数，确保有足够的余额覆盖可能的超额使用
            BigDecimal preBillingAmount = estimatedCost.totalCost().multiply(PRE_BILLING_OUTPUT_RATIO);
            
            // 确保最小预扣费金额
            return preBillingAmount.max(MIN_PRE_BILLING_AMOUNT);
            
        } catch (Exception e) {
            log.warn("估算请求成本失败，使用默认最小预扣费金额: {}", e.getMessage());
            return MIN_PRE_BILLING_AMOUNT;
        }
    }

    /**
     * 执行预扣费操作
     * @param userId 用户ID
     * @param estimatedCost 预估成本
     * @throws InsufficientCreditsException 当余额不足时抛出
     */
    private void performPreBilling(Long userId, BigDecimal estimatedCost) throws InsufficientCreditsException {
        try {
            // 尝试扣减用户余额（预扣费）
            userService.deductUserCredits(userId, estimatedCost);
            log.info("预扣费成功: userId={}, amount={}", userId, estimatedCost);
            
            // 在请求上下文中记录预扣费金额，用于后续退费或补扣
            RequestContext context = RequestContextHolder.getContext();
            context.setPreBilledAmount(estimatedCost);
            
        } catch (InsufficientCreditsException e) {
            log.warn("用户余额不足，无法完成预扣费: userId={}, requiredAmount={}, error={}", 
                    userId, estimatedCost, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("预扣费失败: userId={}, amount={}, error={}", userId, estimatedCost, e.getMessage(), e);
            throw new BusinessException(ErrorCodeConstants.INTERNAL_ERROR, "预扣费失败: " + e.getMessage());
        }
    }

    /**
     * 准备请求上下文并返回客户端信息
     * 包含路由、模型转换和上下文设置等公共逻辑
     * @param request 统一聊天请求
     * @return 客户端信息
     */
    private RoutingService.ClientWithInfo beforeRequest(UnifiedChatRequest request) {
        String modelKey = request.getModel();
        // 路由到合适的供应商
        RoutingService.ClientWithInfo clientWithInfo = routingService.routeAndClient(modelKey);
        if (clientWithInfo == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "No available provider for model: " + modelKey);
        }
        // 替换模型名为供应商模型Key
        request.setModel(clientWithInfo.getVendorModel().getVendorModelKey());
        
        // 设置请求上下文
        RequestContext ctx = RequestContextHolder.getContext();
        ctx.setProviderId(clientWithInfo.getProvider().getId());
        ctx.setVendorModel(clientWithInfo.getVendorModel());
        
        // 获取当前用户ID
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.UNAUTHORIZED, "User not authenticated");
        }
        
        // 估算请求成本并进行预扣费
        try {
            BigDecimal estimatedCost = estimateRequestCost(request, clientWithInfo.getVendorModel());
            log.debug("预估请求成本: userId={}, model={}, estimatedCost={}", 
                    userId, modelKey, estimatedCost);
            
            // 执行预扣费
            performPreBilling(userId, estimatedCost);
            
        } catch (InsufficientCreditsException e) {
            // 余额不足，直接抛出异常，请求不会被执行
            log.warn("用户余额不足，请求被拒绝: userId={}, model={}, error={}", 
                    userId, modelKey, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 预扣费过程中的其他异常，记录日志但允许请求继续
            // 这样可以避免因为预扣费失败而影响正常的请求处理
            log.error("预扣费过程出现异常，允许请求继续执行: userId={}, model={}, error={}", 
                    userId, modelKey, e.getMessage(), e);
        }
        
        return clientWithInfo;
    }

}