package com.oneDatashare.transfer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {
    @GetMapping(value="/hello/hello2")
    public String say(){
        return "Hello";
    }

}
