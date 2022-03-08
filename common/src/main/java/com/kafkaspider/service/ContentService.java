package com.kafkaspider.service;

import org.openqa.selenium.WebDriver;

import java.util.Date;

public interface ContentService {

    WebDriver getDriver();

    //等待页面加载完毕
    void wait(WebDriver chrome, String url);

    //抓取主要内容的方法
    String getMainContent(WebDriver chrome, String url);

    //抓取标题的方法
    String getTitle(WebDriver chrome, String url);

    //抓取标题的方法
    String getTag(WebDriver chrome, String url);

    //抓取时间的方法
    Date getTime(WebDriver chrome, String url);

    //抓取浏览量的方法
    Integer getView(WebDriver chrome, String url);
}
