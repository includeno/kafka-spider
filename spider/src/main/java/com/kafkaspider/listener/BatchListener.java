package com.kafkaspider.listener;

import com.kafkaspider.entity.UrlRecord;

public interface BatchListener {

    public void resolveNormal(UrlRecord record);
    public void resolveError(UrlRecord record);
}
