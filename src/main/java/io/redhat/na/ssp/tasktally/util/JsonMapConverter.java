package io.redhat.na.ssp.tasktally.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Converter(autoApply = true)
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) {
                return "{}";
            }
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize json", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return Collections.emptyMap();
            }
            return MAPPER.readValue(dbData, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to deserialize json", e);
        }
    }
}
