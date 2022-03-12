package com.kafkaspider.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SeleniumConfig {

    public static WebDriver getWebDriver(){
        return getWebDriver(false);
    }

    //https://zhuanlan.zhihu.com/p/402474266
    public static WebDriver getWebDriver(boolean fast){
        ChromeOptions chromeOptions = new ChromeOptions();
        if(SystemConfig.isLinux()){
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-extensions");//禁止加载插件
            chromeOptions.addArguments("blink-settings=imagesEnabled=false");//禁止加载图片
            chromeOptions.addArguments("--disable-software-rasterizer");
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            Map<String, Object> prefs = new HashMap<String, Object>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            chromeOptions.setExperimentalOption("prefs", prefs);

            String UserAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51";
            chromeOptions.addArguments("User-Agent="+ UserAgent);
        }
        else if(SystemConfig.isWindows()){
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-extensions");//禁止加载插件
            chromeOptions.addArguments("blink-settings=imagesEnabled=false");//禁止加载图片
            chromeOptions.addArguments("--disable-software-rasterizer");
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            Map<String, Object> prefs = new HashMap<String, Object>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            chromeOptions.setExperimentalOption("prefs", prefs);

            String UserAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51";
            chromeOptions.addArguments("User-Agent="+ UserAgent);
        }
        else if(SystemConfig.isMac()){
            chromeOptions.addArguments("--headless");

            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-extensions");//禁止加载插件
            chromeOptions.addArguments("blink-settings=imagesEnabled=false");//禁止加载图片
            chromeOptions.addArguments("--disable-software-rasterizer");
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            Map<String, Object> prefs = new HashMap<String, Object>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            chromeOptions.setExperimentalOption("prefs", prefs);

            String UserAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51";
            chromeOptions.addArguments("User-Agent="+ UserAgent);
        }
        if(fast){
            //https://blog.csdn.net/yexiaomodemo/article/details/99958509
            chromeOptions.setCapability("pageLoadStrategy","none");
        }

        //设置为 headless 模式 （必须）
        //chromeOptions.addArguments("--headless");
        //设置浏览器窗口打开大小  （非必须）
        //chromeOptions.addArguments("--window-size=1920,1080");
        return new ChromeDriver(chromeOptions);
    }
}
