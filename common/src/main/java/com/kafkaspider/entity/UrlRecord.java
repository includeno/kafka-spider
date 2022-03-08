package com.kafkaspider.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UrlRecord {
    String url;
    String title;//文章标题
    String content;//文章主体内容
    String tag;//文章显示标注的tag 不一定存在
    Date time;//文章时间
    Integer view=-1;//文章浏览量 -1代表无法获取浏览量数据
    Integer valid=1;//1表示正常 0表示无法获取文章信息 2表示短文章
}
