package org.example.sso.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/30 10:18 上午
 */
@RestController
@Slf4j
public class OkController {

    @RequestMapping("/ok")
    public String ok() throws InterruptedException {
        log.info(Thread.currentThread().getName() + " invoked ok !");
        TimeUnit.SECONDS.sleep(1);
        return "ok";
    }

    @RequestMapping("/timeout")
    public String timeout(int timeout) throws InterruptedException {
        log.info(Thread.currentThread().getName() + " invoked timeout {}s !", timeout);
        TimeUnit.SECONDS.sleep(timeout);
        return "ok";
    }

    @RequestMapping("/monitor")
    public Map<String, String> monitor(String education, String house, String fund) {
        Map<String, String> res = new HashMap<>(3);
        res.put("education", education);
        res.put("house", house);
        res.put("fund", fund);
        return res;
    }
}
