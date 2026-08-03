package com.financeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class FinanceflowApplication {

    public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        System.out.println("TZ   = " + TimeZone.getDefault().getID());
        System.out.println("Zone = " + ZoneId.systemDefault());

        SpringApplication.run(FinanceflowApplication.class, args);
    }
}