package org.example.sso.web;

import org.example.sso.service.ReplayHelloService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/30 10:18 上午
 */
@RestController
public class OkController {

    @Resource
    private ReplayHelloService replayHelloService;

    @RequestMapping("a")
    public void hello(){
        replayHelloService.hello();
    }

    @RequestMapping("/ok")
    public String ok(HttpServletRequest request, HttpServletResponse response) {

        return "ok";
    }

    @RequestMapping("/sleep")
    public String ok(HttpServletRequest request, HttpServletResponse response, Integer sleep) throws InterruptedException {
        if (sleep != null) {
            TimeUnit.SECONDS.sleep(sleep);
        }
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

    @PostMapping("/identifyserver/public/getUserInfo")
    @ResponseBody
    public String getInfo(@RequestBody Map<String, String> params) {
        return "{\"isSuccess\":true,\"isException\":false,\"emplInfo\":{\"emplName\":\"admin\"}}";
    }

}
