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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ZhihuzhuanlanService implements ContentService, MatchService, CleanService {
    public static final String[] patterns = new String[]{
            "https://zhuanlan.zhihu.com/p/(.+)",//https://zhuanlan.zhihu.com/p/88403925
            "https://www.zhihu.com/column/p/(.+)",//https://www.zhihu.com/column/p/24172120
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
        WebDriver chrome = SeleniumConfig.getWebDriver();
        return chrome;
    }

    @Override
    public void wait(WebDriver chrome, String url) {
        log.info("wait began");
        try {
            //等待弹窗加载完毕
            WebDriverWait wait = new WebDriverWait(chrome, 30, 1);
            //如果有注册弹窗 关闭Modal-closeButton
            WebElement searchInput = wait.until(new ExpectedCondition<WebElement>() {
                @Override
                public WebElement apply(WebDriver text) {
                    return text.findElement(By.className("Modal-closeButton"));
                }
            });
            searchInput.click();
            //如果有关注弹窗 关闭Modal-closeButton
            List<WebElement> elements = chrome.findElements(By.className("Modal-closeButton"));
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).click();
            }
            log.info("wait completed");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getMainContent(WebDriver chrome, String url) {
        //直接转到网页底部 加载所有文章信息 参考 https://zhuanlan.zhihu.com/p/38366833
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WebElement content = chrome.findElement(By.className("css-1yuhvjn"));
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
        WebElement content = chrome.findElement(By.className("Post-Title"));
        String ans = content.getText();
        if (ans != null && !ans.equals("")) {
            log.info("getTitle completed" + ans);
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
        //class ContentItem-time
        //发布于 2019-10-20 10:23

        //拉到页面底部
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) chrome;
        javascriptExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date res = new Date();
        WebElement content = chrome.findElement(By.className("ContentItem-time"));
        if (content == null) {
            log.error("getTime error: element = null");
            return res;
        }
        String ans = content.getText();

        if (ans != null && !ans.equals("")) {
            ans = ans.split(" ")[1] + " " + ans.split(" ")[2];
            res = GlobalDateUtil.convert2(ans);
        } else {
            log.error("getTime error:time==null");
        }
        log.info("getTime completed " + res.toString());
        return res;
    }

    @Override
    public Integer getView(WebDriver chrome, String url) {
        //非公开数据
        Integer view=-1;
        return view;
    }

    @Override
    public String cleanUrl(String url) {
        return url;
    }
}

