package com.kafkaspider.controller;

import com.kafkaspider.request.SubmitRequest;
import com.kafkaspider.service.SubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpiderTaskController {

    @Autowired
    SubmitService submitService;

    @PostMapping("/task")
    public void submitSpiderTask(SubmitRequest request){
        submitService.submitFunction(request);
    }
}
