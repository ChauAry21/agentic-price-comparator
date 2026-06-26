package com.agenticprice.tracking.api;

import lombok.Data;

@Data
public class CreateTrackedQueryRequest {
    private String email;
    private String rawQuery;
}
