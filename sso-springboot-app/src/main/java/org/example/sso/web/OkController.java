package org.example.sso.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

    @RequestMapping("/monitor")
    public Map<String, String> monitor(String education, String house, String fund) {
        Map<String, String> res = new HashMap<>(3);
        res.put("education", education);
        res.put("house", house);
        res.put("fund", fund);
        return res;
    }
}
