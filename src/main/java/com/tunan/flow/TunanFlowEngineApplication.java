package com.tunan.flow;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tunan"})
public class TunanFlowEngineApplication {


    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        SpringApplication.run(TunanFlowEngineApplication.class, args);
    }


}
