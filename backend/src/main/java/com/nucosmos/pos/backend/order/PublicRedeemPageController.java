package com.nucosmos.pos.backend.order;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicRedeemPageController {

    @GetMapping({"/redeem", "/redeem/", "/redeem/{token:[a-zA-Z0-9]+}"})
    public String redeemPage() {
        return "forward:/redeem/index.html";
    }
}
