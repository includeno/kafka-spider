package com.kafkaspider.util;

import com.kafkaspider.service.MatchService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UrlFilter {

    //筛选出符合条件的URl
    public static List<String> filter(List<String> list) throws NoSuchMethodException {
        List<String> ans = new ArrayList<>();

        for (String url : list) {
            url=formatUrl(url);
            for (Class c : MatchHelper.impls) {
                Method match = c.getMethod("match", new Class[]{String.class});
                //spring获取实例
                MatchService service = (MatchService) SpringContextUtil.getContext().getBean(c);
                try {
                    boolean result = (boolean) match.invoke(service, url);
                    if (result == true) {
                        ans.add(url);
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return ans.stream().distinct().collect(Collectors.toList());
    }

    public static String formatUrl(String url){
        if(url.endsWith("/")){
            url=url.substring(0,url.length()-1);
        }
        if(url.contains("?")){
            url=url.split("/?")[0];
        }
        if(url.contains("#")){
            url=url.split("#")[0];
        }
        return url;
    }
}
