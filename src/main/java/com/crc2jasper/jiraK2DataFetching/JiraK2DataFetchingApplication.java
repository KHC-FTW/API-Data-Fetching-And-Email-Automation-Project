package com.crc2jasper.jiraK2DataFetching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class JiraK2DataFetchingApplication {
	public static void main(String[] args) {
		SpringApplication.run(JiraK2DataFetchingApplication.class, args);
		SystemIni.readJsonConfigFile(args);
		SystemIni.setupPromotionReleaseConfig();
	}
}