package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MdrUrlsResultItem(String document_id, String file_name, String url, List<MdrUrlsResultItem> grouped) { }
