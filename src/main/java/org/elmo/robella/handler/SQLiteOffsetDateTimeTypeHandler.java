package org.elmo.robella.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * MyBatis TypeHandler for converting between OffsetDateTime (Java) and TEXT (SQLite)
 * This handler is specifically for SQLite databases to handle OffsetDateTime fields
 * that are stored as TEXT in ISO 8601 format in the database.
 */
@MappedTypes(OffsetDateTime.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public class SQLiteOffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    // SQLite datetime('now') returns format like: "2023-10-01 12:34:56"
    private static final DateTimeFormatter SQLITE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType) throws SQLException {
        // 将OffsetDateTime转换为UTC时间并格式化为ISO 8601字符串格式
        ps.setString(i, parameter.withOffsetSameInstant(ZoneOffset.UTC).format(ISO_FORMATTER));
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convertToOffsetDateTime(value);
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convertToOffsetDateTime(value);
    }

    @Override
    public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convertToOffsetDateTime(value);
    }

    private OffsetDateTime convertToOffsetDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as ISO 8601 with offset (e.g., "2023-10-01T12:00:00Z")
            return OffsetDateTime.parse(value, ISO_FORMATTER);
        } catch (Exception e) {
            // Try SQLite datetime format (e.g., "2023-10-01 12:34:56")
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(value, SQLITE_FORMATTER);
                // Treat SQLite datetime as UTC
                return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Cannot parse datetime value: " + value, ex);
            }
        }
    }
}