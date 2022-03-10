package com.kafkaspider.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.kafkaspider.entity.SpiderRecord;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.service.CommonPageService;
import com.kafkaspider.service.sql.SpiderRecordService;
import com.kafkaspider.util.UrlFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/sql")
public class SQLController {

    @Autowired
    CommonPageService commonPageService;

    @Autowired
    Gson gson;

    @Autowired
    SpiderRecordService spiderRecordService;

    //批量读取网页的主要内容
    @PostMapping("/batch")
    public String batch(@RequestParam("list") List<String> list) throws NoSuchMethodException {
        list = list.stream().distinct().collect(Collectors.toList());//url过滤重复url
        log.info("/submit begin filter" + gson.toJson(list));
        List<String> ans = UrlFilter.filter(list);//筛选出符合条件的URl
        log.info("/submit end filter" + gson.toJson(ans));

        for (String url : list) {
            UrlRecord record = new UrlRecord();
            record.setUrl(url);
            log.info("crawl begin:" + gson.toJson(record));
            record = commonPageService.crawl(record);
            log.info("crawl end:" + gson.toJson(record));
            //读取数据库
            QueryWrapper<SpiderRecord> queryWrapper = new QueryWrapper();
            queryWrapper.eq("url",url);
            SpiderRecord spiderRecord = spiderRecordService.getOne(queryWrapper);
            //数据库中不存在
            if (spiderRecord == null) {
                spiderRecordService.save(SpiderRecord.fromUrlRecord(record));
            }
            else {
                Integer id=spiderRecord.getId();
                spiderRecord=SpiderRecord.update(spiderRecord,record);
                spiderRecord.setId(id);
                spiderRecordService.updateById(spiderRecord);
            }
        }
        return "ok";
    }
}
