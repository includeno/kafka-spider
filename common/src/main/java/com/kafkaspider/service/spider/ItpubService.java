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
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ItpubService implements MatchService, ContentService, CleanService {
    public static final String[] patterns = new String[]{
            "http://blog.itpub.net/(.+)/(.+)",//http://blog.itpub.net/70012008/viewspace-2855203/
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
        WebDriverWait wait = new WebDriverWait(chrome, 30, 1);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.id("doc-content"));
            }
        });
        log.info("wait article completed");
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.id("doc-content"));
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
        WebElement content = chrome.findElement(By.className("preview-title"));
        String ans = content.getText();
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
        String ans = "";

        return ans;
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        //class mess 寻找时间
        List<WebElement> mess = chrome.findElements(By.className("mess"));
        Date res = new Date();
        for(WebElement content:mess){
            String ans = content.getText();
            if (ans != null && !ans.equals("")) {
                res = GlobalDateUtil.convert3(ans);
                if(res!=null){
                    break;
                }
            }
        }
        log.info("getTime completed " + res.toString());
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("icon-see"));
        String ans = content.getText();
        Integer view=-1;
        view=Integer.parseInt(ans);
        log.info("getView completed " + view);
        return view;
    }

    @Override
    public String cleanUrl(String url) {
        return url;
    }
}
