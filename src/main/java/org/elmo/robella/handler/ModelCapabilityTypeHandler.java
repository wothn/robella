package org.elmo.robella.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.elmo.robella.model.entity.ModelCapability;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ModelCapabilityTypeHandler extends BaseTypeHandler<List<ModelCapability>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ModelCapability> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize ModelCapability list", e);
        }
    }

    @Override
    public List<ModelCapability> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return deserializeCapabilities(json);
    }

    @Override
    public List<ModelCapability> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return deserializeCapabilities(json);
    }

    @Override
    public List<ModelCapability> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return deserializeCapabilities(json);
    }

    private List<ModelCapability> deserializeCapabilities(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return MAPPER.readValue(json, new TypeReference<List<ModelCapability>>() {});
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize ModelCapability list", e);
        }
    }
}