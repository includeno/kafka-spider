package com.kafkaspider.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;

public class SeleniumConfig {

    public static WebDriver getWebDriver(){
        return getWebDriver(false);
    }

    public static WebDriver getWebDriver(boolean fast){
        ChromeOptions chromeOptions = new ChromeOptions();
        if(SystemConfig.isLinux()){
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
        }
        else if(SystemConfig.isWindows()){
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        }
        else if(SystemConfig.isMac()){
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
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
