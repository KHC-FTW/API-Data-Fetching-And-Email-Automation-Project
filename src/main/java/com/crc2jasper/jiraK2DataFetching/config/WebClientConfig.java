package com.crc2jasper.jiraK2DataFetching.config;

import org.springframework.web.reactive.function.client.WebClient;

public class WebClientConfig {
    private WebClientConfig() {}
    private static final int MAX_MEMORY_BUFFER_FOR_RESP = 50 * 1024 * 1024;
    public static WebClient customWebClient(){
        return WebClient
                .builder()
                .codecs(item -> item.defaultCodecs()
                        .maxInMemorySize(MAX_MEMORY_BUFFER_FOR_RESP))
                .build();
    }
}
