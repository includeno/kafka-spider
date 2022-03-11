package com.kafkaspider.service;

import com.google.gson.Gson;
import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.util.MatchHelper;
import com.kafkaspider.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.openqa.selenium.By;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CommonPageService {
    @Autowired
    OkHttpClient httpClient;

    @Autowired
    Gson gson;

    //爬取网页的主要信息
    public UrlRecord crawl(UrlRecord record) {
        String url = record.getUrl();
        List<Class> clazzs = MatchHelper.impls;
        //打印Class对象
//        for (Class cla : clazzs) {
//            log.info("实现类:" + cla.getClass());
//        }
        for (Class c : clazzs) {
            WebDriver chrome = null;
            try {
                Method match = c.getMethod("match", new Class[]{String.class});
                //spring获取实例
                ContentService service = (ContentService) SpringContextUtil.getContext().getBean(c);
                //仅使用符合条件的爬虫
                if ((boolean) match.invoke(service, url) == true) {
                    log.info("match:" + c.getClass().getName());
                    //使用特定网站专用的浏览器开启方法
                    Method getDriver = c.getMethod("getDriver", new Class[]{});
                    chrome = (WebDriver) getDriver.invoke(service);

                    chrome.get(url);

                    try {
                        Method wait = c.getMethod("wait", new Class[]{WebDriver.class, String.class});
                        wait.invoke(service, chrome, url);

                        Method getTitle = c.getMethod("getTitle", new Class[]{WebDriver.class, String.class});
                        String title = (String) getTitle.invoke(service, chrome, url);
                        record.setTitle(title);

                        Method getTag = c.getMethod("getTag", new Class[]{WebDriver.class, String.class});
                        String tag = (String) getTag.invoke(service, chrome, url);
                        record.setTag(tag);

                        Method getTime = c.getMethod("getTime", new Class[]{WebDriver.class, String.class});
                        Date time = (Date) getTime.invoke(service, chrome, url);
                        record.setTime(time);

                        Method getMainContent = c.getMethod("getMainContent", new Class[]{WebDriver.class, String.class});
                        String content = (String) getMainContent.invoke(service, chrome, url);
                        record.setContent(content);

                        Method getView = c.getMethod("getView", new Class[]{WebDriver.class, String.class});
                        Integer view = (Integer) getView.invoke(service, chrome, url);
                        record.setView(view);

                        log.warn("record: len+" +record.getContent().length()+" time"+record.getTime()+ " url:"+url);
                    }
                    catch (Exception e){
                        log.error("record error"+url);
                    }
                    finally {
                        log.warn("record return"+url);
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
                log.warn("close chrome"+url);
                if (chrome != null) {
                    chrome.close();
                }
            }
        }
        return record;
    }

}
