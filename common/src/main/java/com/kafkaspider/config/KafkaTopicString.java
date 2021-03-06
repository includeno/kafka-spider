package com.kafkaspider.config;

public class KafkaTopicString {

    //spider
    public static final String spidertask ="spidertask-20220312";//spidertask
    public static final String spidertask_slow ="spidertask-slow-20220312";//spidertask-slow
    public static final String spiderresult="spiderresult-20220312";//spiderresult

    //Spark监听

    public static final String updateSpark ="updateSpark";//跳过爬虫部分直接获取最新爬取结果进行Spark数据分析步骤
    public static final String sparkPairAnalyze="sparkPairAnalyze";//Spark 分析原创文章和非原创文章配对任务
    public static final String sparkPairAnalyzeResult="sparkPairAnalyzeResult";//Spark 分析原创文章和非原创文章配对 处理结果
}
