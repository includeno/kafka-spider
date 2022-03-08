package com.kafkaspider.controller;

import com.kafkaspider.request.SubmitRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpiderTaskController {

    @PostMapping("/task")
    public void submitSpiderTask(SubmitRequest request){

    }
}
