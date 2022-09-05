package org.example.sso;

import org.example.sso.web.OkController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/30 10:16 上午
 */
@SpringBootApplication
@ComponentScan(basePackageClasses = MainApplication.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = OkController.class))
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
