package com.kafkaspider.service.spider;

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

@Service
@Slf4j
public class Article163Service implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://www.163.com/dy/article/(.+)",
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
        WebDriverWait wait = new WebDriverWait(chrome, 30, 10);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.className("post_body"));
            }
        });
        log.info("wait article completed");
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("post_body"));
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

    @Override
    public String getTitle(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("post_title"));
        String ans = content.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getTitle completed:" + ans);
            return ans;
        } else {
            log.error("getTitle error:" + ans);
            return "";
        }
    }

    @Override
    public String getTag(WebDriver chrome, String url) {
        String ans = "";
        return ans;
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");

        WebDriverWait wait = new WebDriverWait(chrome, 30, 10);
        WebElement time = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.className("post_info"));
            }
        });
        String ans = time.getText();
        log.info("time before:"+time.getText()+" "+"time after:"+ans);

        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convert3(ans);
        }
        log.info("getTime completed:" + res);
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
