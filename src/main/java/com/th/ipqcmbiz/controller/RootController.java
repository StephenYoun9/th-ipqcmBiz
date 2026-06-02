package com.th.ipqcmbiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 根路径控制器
 */
@Controller
public class RootController {

    /**
     * 根路径重定向到登录页
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/index.html";
    }
}