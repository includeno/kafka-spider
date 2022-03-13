package com.kafkaspider.controller;

import com.google.gson.Gson;
import com.kafkaspider.util.UrlFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class FilterController {

    @Autowired
    Gson gson;

    @PostMapping("/filter")
    public List<String> filter(@RequestParam("list") List<String> list) {
        //去重
        HashSet<String> set=new HashSet<>();
        set.addAll(list);
        list = set.stream().distinct().collect(Collectors.toList());//url过滤重复url

        log.info("/submit begin filter" + gson.toJson(list));
        List<String> ans = new ArrayList<>();//筛选出符合条件的URl
        try {
            ans = UrlFilter.filter(list);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        log.info("/submit end filter" + gson.toJson(ans));

        return ans;
    }
}
