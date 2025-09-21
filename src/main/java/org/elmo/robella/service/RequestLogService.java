package org.elmo.robella.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.mapper.RequestLogMapper;
import org.elmo.robella.model.entity.RequestLog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService extends ServiceImpl<RequestLogMapper, RequestLog> {

    private final RequestLogMapper requestLogMapper;

    public RequestLog createRequestLog(RequestLog requestLog) {
        return createRequestLogSync(requestLog);
    }

    public RequestLog createRequestLogSync(RequestLog requestLog) {
        // Set creation time if not already set
        if (requestLog.getCreatedAt() == null) {
            requestLog.setCreatedAt(OffsetDateTime.now());
        }

        boolean success = save(requestLog);
        if (success) {
            log.debug("Request log saved: {}", requestLog.getId());
            return requestLog;
        } else {
            log.error("Failed to save request log");
            throw new RuntimeException("Failed to save request log");
        }
    }

    public List<RequestLog> getUserLogs(Long userId) {
        LambdaQueryWrapper<RequestLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RequestLog::getUserId, userId)
               .orderByDesc(RequestLog::getCreatedAt);
        return list(wrapper);
    }

    public List<RequestLog> getUserLogsBetweenDates(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RequestLog::getUserId, userId)
               .between(RequestLog::getCreatedAt, startTime, endTime)
               .orderByDesc(RequestLog::getCreatedAt);
        return list(wrapper);
    }

    public List<RequestLog> getApiKeyLogs(Long apiKeyId) {
        LambdaQueryWrapper<RequestLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RequestLog::getApiKeyId, apiKeyId)
               .orderByDesc(RequestLog::getCreatedAt);
        return list(wrapper);
    }

    public List<RequestLog> getRecentLogs() {
        LambdaQueryWrapper<RequestLog> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(RequestLog::getCreatedAt)
               .last("LIMIT 100");
        return list(wrapper);
    }

    public Long getUserRequestCount(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LambdaQueryWrapper<RequestLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RequestLog::getUserId, userId)
               .between(RequestLog::getCreatedAt, startTime, endTime);
        return count(wrapper);
    }

    public Long getUserTotalTokens(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogMapper.sumTotalTokensByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public BigDecimal getUserTotalCost(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogMapper.sumTotalCostByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }
}