package com.kafkaspider.service.sql;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kafkaspider.entity.SpiderRecord;
import com.kafkaspider.mapper.SpiderRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpiderRecordServiceImpl extends ServiceImpl<SpiderRecordMapper, SpiderRecord> implements SpiderRecordService {
    @Autowired
    SpiderRecordMapper spiderRecordMapper;

    public int getLastId(String url){
        return spiderRecordMapper.getLastId(url);
    }
}
