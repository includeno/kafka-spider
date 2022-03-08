package com.kafkaspider.response;

import lombok.Data;

@Data
public class Result {

    public String message;
    public Integer code;
    //data 由子类提供
}
