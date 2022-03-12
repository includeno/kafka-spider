package com.kafkaspider.service;

import com.google.gson.Gson;
import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.util.MatchHelper;
import com.kafkaspider.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
@Service
public class CommonPageService {
    @Autowired
    OkHttpClient httpClient;

    @Autowired
    Gson gson;


    public UrlRecord crawl(UrlRecord record) {
        return this.crawl(record,30,3,5);
    }

    //爬取网页的主要信息
    public UrlRecord crawl(UrlRecord record,Integer pageLoadTimeout,Integer scriptTimeout,Integer implicitlyWait) {
        ConcurrentHashMap<String,Long> concurrentHashMap=new ConcurrentHashMap<>();
        String url = record.getUrl();
        List<Class> clazzs = MatchHelper.impls;
        for (Class c : clazzs) {
            WebDriver chrome = null;
            try {
                Method match = c.getMethod("match", new Class[]{String.class});
                //spring获取实例
                ContentService service = (ContentService) SpringContextUtil.getContext().getBean(c);
                //仅使用符合条件的爬虫
                if ((boolean) match.invoke(service, url) == true) {
                    concurrentHashMap.put("start",System.currentTimeMillis());
                    log.info("match:" + c.getClass().getName());
                    //使用特定网站专用的浏览器开启方法
                    Method getDriver = c.getMethod("getDriver", new Class[]{});
                    chrome = (WebDriver) getDriver.invoke(service);

                    chrome.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
                    chrome.manage().timeouts().setScriptTimeout(scriptTimeout,TimeUnit.SECONDS);
                    chrome.manage().timeouts().implicitlyWait(implicitlyWait, TimeUnit.SECONDS);
                    try
                    {
                        chrome.get(url);
                    }
                    catch (Exception ee){
                        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
                        javascriptExecutor.executeScript("window.stop()");
                    }
                    log.info("driver-get "+(System.currentTimeMillis()-concurrentHashMap.get("start")));
                    try {
                        Method wait = c.getMethod("wait", new Class[]{WebDriver.class, String.class});
                        wait.invoke(service, chrome, url);
                        log.info("driver-wait "+(System.currentTimeMillis()-concurrentHashMap.get("start")));

                        Method getTitle = c.getMethod("getTitle", new Class[]{WebDriver.class, String.class});
                        String title = (String) getTitle.invoke(service, chrome, url);
                        record.setTitle(title);
                        log.info("driver-getTitle "+(System.currentTimeMillis()-concurrentHashMap.get("start")));

                        Method getTag = c.getMethod("getTag", new Class[]{WebDriver.class, String.class});
                        String tag = (String) getTag.invoke(service, chrome, url);
                        record.setTag(tag);

                        Method getTime = c.getMethod("getTime", new Class[]{WebDriver.class, String.class});
                        Date time = (Date) getTime.invoke(service, chrome, url);
                        record.setTime(time);
                        log.info("driver-getTime "+(System.currentTimeMillis()-concurrentHashMap.get("start")));

                        Method getMainContent = c.getMethod("getMainContent", new Class[]{WebDriver.class, String.class});
                        String content = (String) getMainContent.invoke(service, chrome, url);
                        record.setContent(content);
                        log.info("driver-getMainContent "+(System.currentTimeMillis()-concurrentHashMap.get("start")));

                        Method getView = c.getMethod("getView", new Class[]{WebDriver.class, String.class});
                        Integer view = (Integer) getView.invoke(service, chrome, url);
                        record.setView(view);
                        log.info("driver-getView "+(System.currentTimeMillis()-concurrentHashMap.get("start")));

                        log.warn("record: len+" +record.getContent().length()+" time"+record.getTime());
                    }
                    catch (Exception e){
                        log.warn("CommonPageService Exception:");
                        e.printStackTrace();
                    }
                    finally {
                        break;//在找到匹配的爬虫之后跳出循环节省时间
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } finally {
                if (chrome != null) {
                    chrome.close();
                }
            }
        }
        return record;
    }

}
