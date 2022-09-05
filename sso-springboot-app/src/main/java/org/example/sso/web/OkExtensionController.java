package org.example.sso.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author wenbo.zhang
 * @date 2022/7/25 09:24
 */
public class OkExtensionController extends OkController {

    @Override
    public String ok(HttpServletRequest request, HttpServletResponse response) {
        return "ok2";
    }
}
