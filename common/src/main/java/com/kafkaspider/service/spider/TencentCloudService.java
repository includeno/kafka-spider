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
public class TencentCloudService implements MatchService, ContentService, CleanService {
    public static final String[] patterns = new String[]{
            "https://cloud.tencent.com/developer/article/(.+)",//https://cloud.tencent.com/developer/article/1927707
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
                return text.findElement(By.className("J-articlePanel"));
            }
        });
        log.info("wait article completed");

        //拉到页面底部
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        //class nP21pp 展开阅读全文
        try {
            WebElement button = chrome.findElement(By.className("toggle-link"));
            if (button != null) {
                log.warn("检测到阅读原文按钮");
                button.click();
            }
        } catch (NoSuchElementException e) {
            //e.printStackTrace();

        }
        finally {
            return;
        }
    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        //class J-articleContent
        WebElement content = chrome.findElement(By.className("J-articleContent"));
        String ans = content.getText();
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
        //class J-articleTitle
        WebElement title = chrome.findElement(By.className("J-articleTitle"));
        String ans = title.getText();
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
        //class col-tag数组
        String ans = "";
//        try {
//            List<WebElement> tags = chrome.findElements(By.className("col-tag"));
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
        return ans;
    }

    @Override
    public Date getTime(WebDriver chrome, String url) {
        //class article-info -> tag time
        List<WebElement> list = chrome.findElements(By.className("article-info"));
        Date res = null;
        for(WebElement element:list){
            try {
                WebElement timeElement=element.findElement(By.tagName("time"));
                if(timeElement!=null){

                    String ans = timeElement.getAttribute("title");
                    System.out.println("timeElement.getAttribute():"+ans);

                    if (ans != null && !ans.equals("")) {
                        res = GlobalDateUtil.convert3(ans);
                    }
                    log.info("getTime completed:" + res);
                    return res;
                }
            }
            catch (Exception e){
                continue;
            }
        }
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        List<WebElement> list = chrome.findElements(By.className("article-info"));
        Integer view=-1;
        for(WebElement element:list){
            if(element!=null){
                String ans = element.getText();
                log.warn("getView:"+ans);
                if(ans.startsWith("阅读")){
                    try {
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
                        log.info("getView error ");
                        continue;
                    }
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
