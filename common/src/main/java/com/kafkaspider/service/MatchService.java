package com.kafkaspider.service;

public interface MatchService {

    //验证URL和爬虫匹配
    boolean match(String url);
}
