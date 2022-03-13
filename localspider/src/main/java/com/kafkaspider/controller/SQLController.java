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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
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
        //去重
        HashSet<String> set=new HashSet<>();
        set.addAll(list);
        list = set.stream().distinct().collect(Collectors.toList());//url过滤重复url

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

    @PostMapping("/multi/batch")
    public String multibatch(@RequestParam("list") List<String> list) {
        //去重
        HashSet<String> set=new HashSet<>();
        set.addAll(list);
        List<String> urllist = set.stream().distinct().collect(Collectors.toList());//url过滤重复url

        log.info("/submit begin filter" + gson.toJson(urllist));
        List<String> ans = new ArrayList<>();//筛选出符合条件的URl
        try {
            ans = UrlFilter.filter(urllist);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        log.info("/submit end filter" + gson.toJson(ans));

        final Integer divide=8;
        double rounds=Math.ceil(urllist.size()*1.0/divide);
        System.out.println("rounds:"+rounds);
        ConcurrentHashMap<String,Long> concurrentHashMap=new ConcurrentHashMap<>();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(divide, divide, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
        for(int i=0;i< (int)rounds;i++){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            concurrentHashMap.put("start",System.currentTimeMillis());

            List<Callable<UrlRecord>> tasks=new ArrayList<>();
            List<Future<UrlRecord>> futures=new ArrayList<>();
            int count=0;
            for(int g=0;g<divide&&i*divide+g<urllist.size();g++){
                count++;
            }
            System.out.println("count:"+count);
            CountDownLatch downLatch=new CountDownLatch(count);
            for(int g=0;g<divide&&i*divide+g<urllist.size();g++){
                log.info("new round:"+list.get(i*divide+g));
                Callable<UrlRecord> task = getTask(downLatch, list.get(i*divide+g));
                tasks.add(task);
            }

            for(Callable<UrlRecord> task:tasks){
                Future<UrlRecord> future=executor.submit(task);
                futures.add(future);
            }

            try {
                downLatch.await();
            } catch (InterruptedException e) {
                log.warn("downLatch InterruptedException");
            }
            List<UrlRecord> records=new ArrayList<>();
            for(Future<UrlRecord> future:futures){
                try {
                    records.add(future.get());
                    log.info("records.add");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            for(UrlRecord record:records){
                //读取数据库
                QueryWrapper<SpiderRecord> queryWrapper = new QueryWrapper();
                queryWrapper.eq("url",record.getUrl());
                SpiderRecord spiderRecord = spiderRecordService.getOne(queryWrapper);
                //数据库中不存在
                if (spiderRecord == null) {
                    log.info("save "+record.getUrl());
                    spiderRecordService.save(SpiderRecord.fromUrlRecord(record));
                }
                else {
                    log.info("update "+record.getUrl());
                    Integer id=spiderRecord.getId();
                    spiderRecord=SpiderRecord.update(spiderRecord,record);
                    spiderRecord.setId(id);
                    spiderRecordService.updateById(spiderRecord);
                }
            }
            log.info("round elapse: "+(System.currentTimeMillis()-concurrentHashMap.get("start")));
        }
        return "ok";
    }

    public Callable<UrlRecord> getTask(CountDownLatch countDownLatch, String message) {
        Callable<UrlRecord> callable = () -> {
            log.info("spider receive:" + message);
            String url = message;
            Thread.currentThread().setName(Thread.currentThread().getName()+"-"+url);
            UrlRecord record = new UrlRecord();
            record.setUrl(url);
            boolean error=false;
            try {
                log.info("before crawl "+url);
                record=commonPageService.crawl(record);
                log.info("after crawl "+url);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error"+url);
                error=true;
            }
            finally {
                countDownLatch.countDown();
                log.info("return crawl:"+url);
                return record;
            }
        };
        return callable;
    }
}
