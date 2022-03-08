package com.kafkaspider.service.spider;

import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.service.CleanService;
import com.kafkaspider.service.ContentService;
import com.kafkaspider.service.MatchService;
import com.kafkaspider.util.GlobalDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class JianshuService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://www.jianshu.com/p/(.+)",//https://www.jianshu.com/p/f0ad0f80fd2c
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
        WebDriver chrome = SeleniumConfig.getWebDriver(false);
        return chrome;
    }

    @Override
    public void wait(WebDriver chrome, String url) {
        WebDriverWait wait = new WebDriverWait(chrome, 30, 1);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.tagName("article"));
            }
        });
        log.info("wait article completed");

        //拉到页面底部
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        //class nP21pp 展开阅读全文
        try {
            WebElement button = chrome.findElement(By.className("nP21pp"));
            if (button != null) {
                log.warn("检测到阅读原文按钮");
                button.click();
            }
        } catch (NoSuchElementException e) {
            //e.printStackTrace();
            log.warn("Unable to locate element: {\"method\":\"css selector\",\"selector\":\".nP21pp\"}");
        }

    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        //tag article
        //https://www.jianshu.com/p/f0ad0f80fd2c
        WebElement content = chrome.findElement(By.tagName("article"));
        String ans = "";
        if (content == null) {
            log.error("getMainContent error: element = null");
            return ans;
        }
        ans = content.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getMainContent completed:" + ans.length());
            return ans;
        } else {
            log.error("getMainContent error:" + ans);
            return "";
        }
    }

    @Override
    public String getTitle(WebDriver chrome, String url) {
        //class _1RuRku
        WebElement content = chrome.findElement(By.className("_1RuRku"));
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
        return "";
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        //2021.11.19 17:08:03
        //tag time
        WebElement content = chrome.findElement(By.tagName("time"));
        String ans = content.getText();
        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convertFull_3(ans);
        }
        log.info("getTime completed:" + res.toString());
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        //寻找 阅读 49,528
        List<WebElement> list = chrome.findElements(By.tagName("span"));
        Integer view=-1;
        for(WebElement content:list){
            String ans = content.getText();
            if(ans.startsWith("阅读")){
                try {
                    ans=ans.split(" ")[1];
                    view=Integer.parseInt(ans);
                    log.info("getView completed " + view);
                    return view;
                }
                catch (Exception e){
                    continue;
                }
            }
        }
        return view;
    }
    @Override
    public String cleanUrl(String url) {
        return null;
    }
}
