package org.elmo.robella.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 汇率服务，用于将不同货币转换为CNY
 */
@Slf4j
@Service
public class ExchangeRateService {
    
    // 预定义的汇率映射 
    // todo 从外部API获取并定期更新
    private final Map<String, BigDecimal> exchangeRates = new ConcurrentHashMap<>(Map.of(
        "CNY", BigDecimal.ONE,
        "USD", new BigDecimal("7.13"),
        "EUR", new BigDecimal("8.349"),
        "GBP", new BigDecimal("9.20"),
        "JPY", new BigDecimal("0.0477"),
        "KRW", new BigDecimal("0.0054")
    ));
    
    /**
     * 将指定货币金额转换为CNY
     * @param amount 金额
     * @param fromCurrency 源货币
     * @return 转换后的CNY金额
     */
    public BigDecimal convertToCNY(BigDecimal amount, String fromCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if ("CNY".equalsIgnoreCase(fromCurrency)) {
            return amount;
        }
        
        BigDecimal rate = exchangeRates.get(fromCurrency.toUpperCase());
        if (rate == null) {
            log.warn("Unsupported currency: {}, using default rate 1.0", fromCurrency);
            rate = BigDecimal.ONE;
        }
        
        return amount.multiply(rate).setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * 更新汇率
     * @param currency 货币代码
     * @param rate 汇率（相对于CNY）
     */
    public void updateExchangeRate(String currency, BigDecimal rate) {
        if (currency != null && rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
            exchangeRates.put(currency.toUpperCase(), rate);
        }
    }
}