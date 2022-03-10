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
public class OschinaService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://my.oschina.net/(.+)/blog/(.+)",//https://my.oschina.net/xcafe/blog/5389937
            "https://my.oschina.net/u/(.+)/blog/(.+)",//https://my.oschina.net/u/729507/blog/78144
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
                return text.findElement(By.className("article-box__content"));
            }
        });
        log.info("wait article completed");
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("article-box__content"));
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
        WebElement content = chrome.findElement(By.className("article-box__title"));
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
        //class item匹配时间
        List<WebElement> list = chrome.findElements(By.className("lm"));
        Date res = null;
        for (WebElement element : list) {
            String ans = element.getText();
            ans=ans.strip();
            if (ans != null && !ans.equals("")) {
                try{
                    log.warn("getTime item:"+ans);
                    res = GlobalDateUtil.convert2_1(ans);
                    break;
                }
                catch (Exception e){
                    continue;
                }
            }
        }
        if(res==null){
            log.error("getTime error no matches");
        }
        else{
            log.info("getTime completed "+res);
        }
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        Integer view=-1;

        List<WebElement> list = chrome.findElements(By.className("lm"));
        for (WebElement element : list) {
            String ans = element.getText();
            if (ans != null && ans.startsWith("阅读数")) {
                try{
                    ans=ans.split(" ")[1];
                    if(ans.endsWith("K")){
                        ans=ans.split("K")[0];
                        Double base=Double.parseDouble(ans);
                        view=(int)(base*1000);
                        log.info("getView completed " + view);
                    }
                    else{
                        view=Integer.parseInt(ans);
                        log.info("getView completed " + view);
                    }
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
        return url;
    }
}
