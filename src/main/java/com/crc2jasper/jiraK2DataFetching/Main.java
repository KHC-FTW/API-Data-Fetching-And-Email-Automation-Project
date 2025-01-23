package com.crc2jasper.jiraK2DataFetching;

import com.crc2jasper.jiraK2DataFetching.service.ScheduleService;
import com.crc2jasper.jiraK2DataFetching.service.AppIniService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class Main {
	public static void main(String[] args) {
		AppIniService.readJsonConfigFile(args);
		AppIniService.setupPromotionReleaseConfig();
		SpringApplication.run(Main.class, args);
		/*
		* For quick test purposes
		* */

//		ScheduleService.manualTestSendBiweeklyEmail_V2_multiThreaded("25-Sep-2024", "2024", "12");
//		ScheduleService.manualTestSendBiweeklyEmail_V2("03-Dec-2024", "2024", "15");
//		ScheduleService.sendUrgentServiceEmail_V2();
		ScheduleService.simulateSendBiweeklyEmail();
	}
}
