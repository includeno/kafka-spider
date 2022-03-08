package com.kafkaspider.util;

import java.net.MalformedURLException;
import java.net.URL;

public class HostUtil {

    //抓取主机名的方法
    String getHost(String url){
        URL resource = null;
        String host="";
        try {
            resource = new URL(url);
            host= resource.getHost();// 获取主机名
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        finally {
            return host;
        }
    }
}
