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
public class IfengService {
    public static final String[] patterns = new String[]{
            "https://(.+).ifeng.com/c/(.+)",
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
                return text.findElement(By.className("body"));
            }
        });
        log.info("wait article completed");
    }

    
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("main_content-3N5v8C0v"));
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
        WebElement content = chrome.findElement(By.className("topic-2zFngUzL"));
        String ans = content.getText();
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
        WebElement time = chrome.findElement(By.className("time-1zG3fh78"));
        String ans = time.getText().replace("年","-").replace("月","-").replace("日","");
        log.info("time before:"+time.getText()+" "+"time after:"+ans);

        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convert3(ans);
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
