package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    private final UserService userService;

    @Autowired
    public HomeViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        int totalUsers = userService.findAll().size();
        model.addAttribute("totalUsers", totalUsers);
        return "auth/login";
    }

}
