package org.example.sso.service;

import org.example.sso.annotation.OverrideBean;

/**
 * @author wenbo.zhang
 * @date 2022/8/4 10:12
 */
@OverrideBean(name = "replayHelloService")
public class ReplayHelloServiceExtension extends ReplayHelloService {


    public void hello(){
        System.out.println("hello a extension");
    }
}
