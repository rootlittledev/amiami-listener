package com.ethero.amiamilistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AmiamiListenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmiamiListenerApplication.class, args);
    }

}
