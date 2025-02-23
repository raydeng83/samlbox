package com.bostonidentity.samlbox.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogoutController {

    @GetMapping("/logout-success")
    public String logoutSuccess(Model model) {
        model.addAttribute("logoutMessage", "You have been successfully logged out!");
        return "logout-success";
    }
}
