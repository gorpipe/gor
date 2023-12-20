package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.exceptions.GorSystemException;

import java.util.List;

public record MdrUrlsResult(String url_type, List<MdrUrlsResultItem> urls) {
    public static MdrUrlsResult fromJSON(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, MdrUrlsResult.class);
        } catch (JsonProcessingException e) {
            throw new GorSystemException("Failed to parse MDR urls result", e);
        }
    }
}