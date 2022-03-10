package com.kafkaspider.service.spider;

import com.kafkaspider.config.SeleniumConfig;
import com.kafkaspider.service.CleanService;
import com.kafkaspider.service.ContentService;
import com.kafkaspider.service.MatchService;
import com.kafkaspider.util.GlobalDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CsdnService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://blog.csdn.net/(.+)/article/details/(.+)",//https://blog.csdn.net/qq_16214677/article/details/84863046
            "https://(.+).blog.csdn.net/article/details/(.+)",//https://gxyyds.blog.csdn.net/article/details/96458591
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
        WebDriverWait wait = new WebDriverWait(chrome, 30, 10);
        WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.tagName("article"));
            }
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("wait article completed");
        Actions action = new Actions(chrome);
        Integer random = new Random(30).nextInt();
        for (int i = 0; i < 10 + random; i++) {
            action.sendKeys(Keys.chord(Keys.DOWN));
        }
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try {
            WebElement content = chrome.findElement(By.className("passport-container"));
            if (content != null) {
                WebElement button = chrome.findElement(By.xpath("//span[contains(text(),'x')]"));
                button.click();
                log.info("wait passport completed");
            }
        } catch (NoSuchElementException exception) {
            log.error("can't find passport");
        }
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.tagName("article"));
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
        WebElement content = chrome.findElement(By.id("articleContentId"));
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
//        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
//        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
//        try {
//            WebElement content = chrome.findElement(By.className("passport-container"));
//            if (content != null) {
//                WebElement button = chrome.findElement(By.xpath("//span[contains(text(),'x')]"));
//                button.click();
//                log.info("wait passport completed");
//            }
//        } catch (NoSuchElementException exception) {
//            log.error("can't find passport");
//        }
//        //数组 tag-link
//
//        try {
//            List<WebElement> tags = chrome.findElements(By.className("tag-link"));
//            if (tags != null && tags.size() > 0) {
//                StringBuffer stringBuffer = new StringBuffer();
//                for (WebElement element : tags) {
//                    stringBuffer.append(element.getText() + " ");
//                }
//                ans = stringBuffer.toString();
//                log.info("getTag completed " + ans);
//            }
//        } catch (NoSuchElementException e) {
//            e.printStackTrace();
//            log.error("getTag error " + ans);
//        }
        String ans = "";
        return ans;
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try {
            WebElement content = chrome.findElement(By.className("passport-container"));
            if (content != null) {
                WebElement button = chrome.findElement(By.xpath("//span[contains(text(),'x')]"));
                button.click();
                log.info("wait passport completed");
            }
        } catch (NoSuchElementException exception) {
            log.error("can't find passport");
        }

        //class time
        WebDriverWait wait = new WebDriverWait(chrome, 30, 10);
        WebElement content = wait.until(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver text) {
                return text.findElement(By.className("time"));
            }
        });
        String ans = content.getText();
        System.out.println("content.getText():"+content.getText());
        //正则匹配获取字符串中的时间 于 2018-04-22 14:44:38 发布

        Date res = new Date();
        if (ans != null && !ans.equals("")) {
            res = GlobalDateUtil.convert3(ans);
        }
        log.info("getTime completed:" + res);
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("read-count"));
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
