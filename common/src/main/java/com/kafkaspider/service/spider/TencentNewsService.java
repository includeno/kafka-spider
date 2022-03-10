package com.kafkaspider.service.spider;

import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.service.CleanService;
import com.kafkaspider.service.ContentService;
import com.kafkaspider.service.MatchService;
import com.kafkaspider.util.GlobalDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TencentNewsService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://(.+).qq.com/omn/20(.+)/(.+)"
    };

    @Override
    public boolean match(String url) {
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            if (p.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public WebDriver getDriver() {
        WebDriver chrome = SeleniumConfig.getWebDriver(true);
        return chrome;
    }

    @Override
    public void wait(WebDriver chrome, String url) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        WebDriverWait wait = new WebDriverWait(chrome, 30, 1);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.id("LeftTool"));
            }
        });
        log.info("wait article completed"+searchInput);
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("content-article"));
        String ans = content.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getMainContent completed:" + ans.length());
            return ans;
        } else {
            log.error("getMainContent error " + ans);
            return "";
        }
    }

    @Override
    public String getTitle(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("LEFT"));
        WebElement title = content.findElement(By.tagName("h1"));
        String ans = title.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getTitle completed " + ans);
            return ans;
        } else {
            log.error("getTitle error " + ans);
            return "";
        }
    }

    @Override
    public String getTag(WebDriver chrome, String url) {
        return "";
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        Date res = new Date();
        try {
            WebElement left = chrome.findElement(By.className("left-stick-wp"));
            if(left==null){
                return null;
            }
            WebElement year = left.findElement(By.className("year"));
            WebElement md = left.findElement(By.className("md"));
            WebElement time = left.findElement(By.className("time"));
            if(year==null||md==null||time==null){
                return null;
            }
            //2022-03-07 20:00
            String ans = year.getText()+"-"+md.getText().split("/")[0]+"-"+md.getText().split("/")[1]+" "+ time.getText();

            if (ans != null && !ans.equals("")) {
                res = GlobalDateUtil.convert2(ans);
            }
            log.info("getTime completed " + res.toString());
        }
        catch (Exception e){
            return null;
        }
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        Integer view=-1;
        log.info("getView completed " + view);
        return view;
    }

    @Override
    public String cleanUrl(String url) {
        return url;
    }
}
