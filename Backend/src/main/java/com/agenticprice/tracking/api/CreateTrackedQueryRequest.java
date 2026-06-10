package com.agenticprice.tracking.api;

import lombok.Data;

@Data
public class CreateTrackedQueryRequest {
    private String rawQuery;
    private String canonicalProductKey;
    private String canonicalProductName;
    private String referenceUrl;
}
