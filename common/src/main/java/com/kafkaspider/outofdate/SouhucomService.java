package com.kafkaspider.outofdate;

import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.service.CleanService;
import com.kafkaspider.service.ContentService;
import com.kafkaspider.service.MatchService;
import com.kafkaspider.util.GlobalDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.regex.Pattern;

@Slf4j
public class SouhucomService  {
    public static final String[] patterns = new String[]{
            "https://www.sohu.com/a/(.+)",
            "http://www.sohu.com/a/(.+)",
    };

    
    public boolean match(String url) {
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            if (p.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    
    public WebDriver getDriver() {
        WebDriver chrome = SeleniumConfig.getWebDriver(false);
        return chrome;
    }

    
    public void wait(WebDriver chrome, String url) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WebDriverWait wait = new WebDriverWait(chrome, 30, 1);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            
            public WebElement apply(WebDriver text) {
                return text.findElement(By.id("mp-editor"));
            }
        });
        log.info("wait article completed");

    }

    
    public String getMainContent(WebDriver chrome, String url) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        //show-all
        try {
            WebElement showMore = chrome.findElement(By.id("showMore"));
            showMore.click();
        }
        catch (Exception e){
            log.warn("no showMore");
        }
        WebElement content = chrome.findElement(By.id("mp-editor"));
        String ans = "";
        if (content == null) {
            log.error("getMainContent error: element = null");
            return ans;
        }
        ans = content.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getMainContent completed: length " + ans.length());
            return ans;
        } else {
            log.error("getMainContent error:" + ans);
            return "";
        }
    }

    
    public String getTitle(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("text-title"));
        WebElement title = content.findElement(By.tagName("h1"));
        if(title==null){
            log.error("getTitle error: No WebElement h1");
            return "";
        }
        String ans = title.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getTitle completed:" + ans);
            return ans;
        } else {
            log.error("getTitle error:" + ans);
            return "";
        }
    }

    
    public String getTag(WebDriver chrome, String url) {
        String ans = "";
        return ans;
    }

    
    public Date getTime(WebDriver chrome, String url) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WebElement time = chrome.findElement(By.id("news-time"));
        String ans = time.getText();
        log.info("time before:"+time.getText()+" "+"time after:"+ans);

        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convert2(ans);
        }
        log.info("getTime completed:" + res);
        return res;
    }

    
    public Integer getView(WebDriver chrome, String url) {
        Integer view=-1;
        log.info("getView completed " + view);
        return view;
    }

    
    public String cleanUrl(String url) {
        return url;
    }
}
