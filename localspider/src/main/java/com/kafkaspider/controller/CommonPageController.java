package com.kafkaspider.controller;

import com.google.gson.Gson;
import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.config.SpiderLimit;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.enums.SpiderCode;
import com.kafkaspider.response.SpiderResponse;
import com.kafkaspider.service.CommonPageService;
import com.kafkaspider.worker.SpiderWorker;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


@Slf4j
@RestController
public class CommonPageController {

    @Autowired
    CommonPageService commonPageService;

    @Autowired
    Gson gson;

    //读取网页的主要内容
    @PostMapping("/crawl")
    public SpiderResponse crawl(@RequestParam("url") String url) {
        long start=System.currentTimeMillis();
        log.info("crawl begin:"+url+" "+start);
        SpiderResponse response=new SpiderResponse();
        UrlRecord record = new UrlRecord();
        record.setUrl(url);
        if (SpiderLimit.spiders.size()<SpiderLimit.countOfSpider&&!SpiderLimit.spiders.contains(url)) {
            SpiderLimit.spiders.add(url);
            log.info("commonPageService crawl begin:"+gson.toJson(record));
            record=commonPageService.crawl(record);
            log.info("commonPageService crawl end:"+gson.toJson(record));
            SpiderLimit.spiders.remove(url);
            if(record!=null&&record.getTitle()!=null&&record.getTitle().length()>0&&record.getContent()!=null&&record.getContent().length()>0){
                response.setCode(SpiderCode.SUCCESS.getCode());
            }
            else {
                response.setCode(SpiderCode.SPIDER_UNREACHABLE.getCode());
            }
        }
        else{
            response.setCode(SpiderCode.SPIDER_COUNT_LIMIT.getCode());//因为爬虫服务数量已满
        }
        response.setRecord(record);
        log.info("crawl end:"+url+" "+(System.currentTimeMillis()-start));
        return response;
    }

    //读取网页的主要内容
    @PostMapping("/factory")
    public void factory(@RequestParam("list") List<String> list) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 20, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));
        CountDownLatch downLatch=new CountDownLatch(list.size());
        try {
            for(String url:list){
                executor.submit(new SpiderWorker(downLatch,url,commonPageService,log));
            }
            downLatch.await(30,TimeUnit.SECONDS);
        }
        catch (Exception e){
            log.error("executor error");
        }
        finally {
            executor.shutdown();
        }

    }

    public Callable<UrlRecord> getTask(String url){
        Callable<UrlRecord> callable = () -> {
            UrlRecord record=new UrlRecord();
            record.setUrl(url);
            try {
                log.info("commonPageService crawl begin:"+gson.toJson(record));
                record=commonPageService.crawl(record);
                log.info("commonPageService crawl end:"+gson.toJson(record));
            }
            catch (Exception e){
                log.error("factoryTask error!");
            }
            finally {

            }
            return record;
        };
        return callable;
    }


}
