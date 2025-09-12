package org.elmo.robella.config;

import io.r2dbc.spi.ConnectionFactory;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.ModelCapability;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.lang.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableR2dbcRepositories
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    @NonNull
    public ConnectionFactory connectionFactory() {
        return this.connectionFactory;
    }

    @Override
    @NonNull
    protected List<Object> getCustomConverters() {
        return Arrays.asList(
                new RoleToIntegerConverter(),
                new IntegerToRoleConverter(),
                new ModelCapabilityWriteConverter(),
                new ModelCapabilityReadConverter());
    }

    @WritingConverter
    static class RoleToIntegerConverter implements Converter<Role, Integer> {
        @Override
        public Integer convert(@NonNull Role source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    static class IntegerToRoleConverter implements Converter<Integer, Role> {
        @Override
        public Role convert(@NonNull Integer source) {
            return Role.fromValue(source);
        }
    }

    @WritingConverter
    public static class ModelCapabilityWriteConverter implements Converter<List<ModelCapability>, Json> {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
    public Json convert(@NonNull List<ModelCapability> source) {
            try {
                return Json.of(MAPPER.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize ModelCapability list", e);
            }
        }
    }

    @ReadingConverter
    public static class ModelCapabilityReadConverter implements Converter<Json, List<ModelCapability>> {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
    public List<ModelCapability> convert(@NonNull Json source) {
            try {
                return MAPPER.readValue(source.asString(), new TypeReference<List<ModelCapability>>() {
                });
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to deserialize ModelCapability list", e);
            }
        }
    }
}
