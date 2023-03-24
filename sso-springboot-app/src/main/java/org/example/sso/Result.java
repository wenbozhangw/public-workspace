package org.example.sso;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/6/10 12:15 下午
 */
public class Result extends AbstractQueuedSynchronizer {
    private boolean isSuccess = true;
    private boolean isException = true;

    private Map<String, String> emplInfo = new HashMap<>(1);

    public Result(String token) {
        emplInfo.put("emplName", token);
    }

}
