package org.example.sso.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/30 10:18 上午
 */
@RestController
public class OkController {

    @RequestMapping("/ok")
    public String ok() {

        return "ok";
    }
}
