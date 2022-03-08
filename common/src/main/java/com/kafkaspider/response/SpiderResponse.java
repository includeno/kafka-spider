package com.kafkaspider.response;

import com.kafkaspider.entity.UrlRecord;
import lombok.Data;

@Data
public class SpiderResponse extends Result {

    public UrlRecord record;
}
