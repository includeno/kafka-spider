package com.kafkaspider.worker;

import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.service.CommonPageService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class SpiderWorker {

    @Autowired
    CommonPageService commonPageService;

    public Callable<UrlRecord> getTask(CountDownLatch countDownLatch,String message,Integer pageLoadTimeout,Integer scriptTimeout,Integer implicitlyWait) {
        Callable<UrlRecord> callable = () -> {
            log.info("spider receive:" + message);
            String url = message;
            UrlRecord record = new UrlRecord();
            record.setUrl(url);
            try {
                log.info("before crawl "+url);
                record=commonPageService.crawl(record,pageLoadTimeout,scriptTimeout,implicitlyWait);
                log.info("after crawl "+url);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error"+url);
            }
            finally {
                countDownLatch.countDown();
                log.info("return crawl:"+url);
                return record;
            }
        };
        return callable;
    }

    public Callable<UrlRecord> getTask(CountDownLatch countDownLatch,String message) {
        Callable<UrlRecord> callable = () -> {
            log.info("spider receive:" + message);
            String url = message;
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
