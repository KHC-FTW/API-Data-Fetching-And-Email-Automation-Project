package com.crc2jasper.jiraK2DataFetching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

public class WebClientConfig {

    private static final int maxMemoryBufferForResp = 50 * 1024 * 1024;

    public static WebClient customWebClient(){
        return WebClient
                .builder()
                .codecs(item -> item.defaultCodecs()
                        .maxInMemorySize(maxMemoryBufferForResp))
                .build();
    }
}
