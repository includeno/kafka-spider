package com.kafkaspider.enums;

public enum SpiderCode {

    SPIDER_COUNT_LIMIT(1),//爬虫服务数量限制
    SPIDER_UNREACHABLE(2),//无法访问网页
    SUCCESS(200),//正常
    ;
    private int code;
    SpiderCode(int code){
        this.code=code;
    }
    public int getCode(){
        return this.code;
    }

}
