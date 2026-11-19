package com.tienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class TiendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiendaApplication.class, args);
    }
}
