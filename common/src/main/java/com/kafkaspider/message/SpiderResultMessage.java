package com.kafkaspider.message;

import com.kafkaspider.entity.UrlRecord;
import lombok.Data;

@Data
public class SpiderResultMessage extends UrlRecord {
    String message;
    Integer code;

    String simhash;

    public static SpiderResultMessage copyUrlRecord(UrlRecord record){
        SpiderResultMessage spiderResultMessage = new SpiderResultMessage();
        spiderResultMessage.setUrl(record.getUrl());
        spiderResultMessage.setContent(record.getContent());
        spiderResultMessage.setTitle(record.getTitle());
        spiderResultMessage.setTime(record.getTime());
        spiderResultMessage.setView(record.getView());
        spiderResultMessage.setValid(record.getValid());
        return spiderResultMessage;
    }
}
