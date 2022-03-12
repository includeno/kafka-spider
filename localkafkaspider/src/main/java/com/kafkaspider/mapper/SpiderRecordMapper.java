package com.kafkaspider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kafkaspider.entity.SpiderRecord;
import org.springframework.stereotype.Component;

@Component
public interface SpiderRecordMapper extends BaseMapper<SpiderRecord> {
    public int getLastId(String url);
}
