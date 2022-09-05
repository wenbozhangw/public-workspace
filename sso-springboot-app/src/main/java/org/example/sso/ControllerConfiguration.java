package org.example.sso;

import org.example.sso.web.OkExtensionController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wenbo.zhang
 * @date 2022/7/25 09:37
 */
@Configuration
public class ControllerConfiguration {

    @Bean
    public OkExtensionController okExtensionController() {
        return new OkExtensionController();
    }
}
