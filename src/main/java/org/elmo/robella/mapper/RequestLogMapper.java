package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.RequestLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLog> {

    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM request_log WHERE user_id = #{userId} AND created_at BETWEEN #{startTime} AND #{endTime}")
    Long sumTotalTokensByUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM request_log WHERE user_id = #{userId} AND created_at BETWEEN #{startTime} AND #{endTime}")
    BigDecimal sumTotalCostByUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    @Select("SELECT * FROM request_log WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at")
    List<RequestLog> findByCreatedAtBetween(@Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    @Select("SELECT * FROM request_log WHERE user_id = #{userId} AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at")
    List<RequestLog> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);
}