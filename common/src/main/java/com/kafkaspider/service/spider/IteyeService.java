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
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IteyeService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://www.iteye.com/blog/(.+)",//https://www.iteye.com/blog/m17165851127-2524064
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
                return text.findElement(By.className("iteye-blog-content-contain"));
            }
        });
        log.info("wait article completed");
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");

    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        WebElement content = chrome.findElement(By.className("iteye-blog-content-contain"));
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
        WebElement content = chrome.findElement(By.className("blog_title"));
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
        //class blog_bottom
        WebElement bottom = chrome.findElement(By.className("blog_bottom"));
        List<WebElement> list = bottom.findElements(By.tagName("li"));

        Date res = null;
        for (WebElement entry : list) {
            String timeStr = entry.getText();
            res = GlobalDateUtil.convert2(timeStr);
            if(res!=null){
                log.info("getTime completed:" + res.toString());
                return res;
            }
        }
        res=new Date();
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        WebElement bottom = chrome.findElement(By.className("blog_bottom"));
        List<WebElement> list = bottom.findElements(By.tagName("li"));
        Integer view=-1;
        for (WebElement entry : list) {
            String ans = entry.getText();
            if(ans.startsWith("浏览")){
                try {
                    ans= ans.split(" ")[1];
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
        return url;
    }
}
