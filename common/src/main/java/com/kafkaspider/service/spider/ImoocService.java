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
public class ImoocService implements MatchService, ContentService, CleanService {
    public static final String[] patterns = new String[]{
            "https://www.imooc.com/article/(.+)",//https://www.imooc.com/article/303392
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
                return text.findElement(By.className("detail-content"));
            }
        });
        log.info("wait article completed");
        //class showMore
        //拉到页面底部
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        //class nP21pp 展开阅读全文
        try {
            WebElement button = chrome.findElement(By.className("showMore"));
            if (button != null) {
                log.warn("检测到showMore按钮");
                button.click();
            }
        } catch (NoSuchElementException e) {
            //e.printStackTrace();
            log.warn("Unable to locate element: {\"method\":\"css selector\",\"selector\":\".showMore\"}");
        }
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("detail-content"));
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
        WebElement content = chrome.findElement(By.className("detail-title"));
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
        List<WebElement> mess = chrome.findElements(By.className("spacer"));
        Date res = new Date();
        for(WebElement content:mess){
            String ans = content.getText();
            if (ans != null && !ans.equals("")) {
                res = GlobalDateUtil.convertFull_2(ans);
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
        WebElement content = chrome.findElement(By.className("spacer-2"));
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
