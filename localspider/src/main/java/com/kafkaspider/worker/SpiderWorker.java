package com.kafkaspider.worker;

import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.service.CommonPageService;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class SpiderWorker implements Callable<UrlRecord> {

    private CountDownLatch downLatch;
    private String url;
    private CommonPageService commonPageService;
    private Logger log;

    public SpiderWorker(CountDownLatch downLatch, String url, CommonPageService commonPageService, Logger log){
        this.downLatch = downLatch;
        this.url = url;
        this.commonPageService=commonPageService;
        this.log=log;
    }

    @Override
    public UrlRecord call() throws Exception {
        UrlRecord record=new UrlRecord();
        record.setUrl(url);
        try {
            log.info("commonPageService crawl begin:"+url);
            record=commonPageService.crawl(record);
            log.info("commonPageService crawl end:"+url);
        }
        catch (Exception e){
            System.out.println("factoryTask error!");
        }
        finally {

        }
        System.out.println(this.url + "活干完了！");
        this.downLatch.countDown();
        return record;
    }
}
