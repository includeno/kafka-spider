package com.kafkaspider.service.sql;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kafkaspider.entity.SpiderRecord;

public interface SpiderRecordService extends IService<SpiderRecord> {
    public int getLastId(String url);
}
