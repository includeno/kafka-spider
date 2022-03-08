package com.kafkaspider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

//数据库实体
@Data
@Accessors(chain = true)
public class SpiderRecord {

    @TableId(value = "id", type = IdType.AUTO)
    Integer id;

    String url="";//文章地址
    String title="";//文章标题
    String tag="";//文章显示的标签
    String content="";//文章内容
    Integer view=-1;//文章浏览量
    Date time;//文章时间
    Integer valid;//记录 有效1 无效0

    Date createTime;
    Date updateTime;


    public static UrlRecord toUrlRecord(SpiderRecord spiderRecord) {
        UrlRecord urlRecord=new UrlRecord();
        urlRecord.setUrl(spiderRecord.getUrl());
        urlRecord.setContent(spiderRecord.getContent());
        urlRecord.setTitle(spiderRecord.getTitle());
        urlRecord.setView(spiderRecord.getView());
        urlRecord.setTag(spiderRecord.getTag());
        urlRecord.setTime(spiderRecord.getTime());
        urlRecord.setValid(spiderRecord.getValid());
        return urlRecord;
    }

}
