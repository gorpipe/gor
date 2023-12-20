package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import java.util.List;

public record MdrUrlsResultItem(String document_id, String file_name, String url, List<MdrUrlsResultItem> grouped) { }
