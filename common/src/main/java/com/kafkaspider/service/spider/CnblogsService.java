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
public class CnblogsService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://www.cnblogs.com/(.+)/p/(.+)",//https://www.cnblogs.com/frankdeng/p/9310684.html
            "https://www.cnblogs.com/(.+)/archive/(.+)",//https://www.cnblogs.com/fengys-moving/archive/2012/05/27/2520549.html
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
                return text.findElement(By.id("post_detail"));
            }
        });
        log.info("wait article completed");
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.id("cnblogs_post_body"));
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
        WebElement content = chrome.findElement(By.id("cb_post_title_url"));
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
        return "";
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        //class time
        WebElement content = chrome.findElement(By.id("post-date"));
        if(content==null){
            return null;
        }
        String ans = content.getText();
        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convert2(ans);
        }
        log.info("getTime completed " + res.toString());
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.id("post_view_count"));
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
