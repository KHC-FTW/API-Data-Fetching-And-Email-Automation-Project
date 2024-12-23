package com.crc2jasper.jiraK2DataFetching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class JiraK2DataFetchingApplication {
	public static void main(String[] args) {
		SystemIni.readJsonConfigFile(args);
		SystemIni.setupPromotionReleaseConfig();
		SpringApplication.run(JiraK2DataFetchingApplication.class, args);
		/*
		* For quick test purposes
		* */
//		EmailScheduler.manualTestSendBiweeklyEmail("23-Dec-2024", "2024", "16");
//		EmailScheduler.sendUrgentServiceEmail();
//		EmailScheduler.simulateSendBiweeklyEmail();
	}
}
