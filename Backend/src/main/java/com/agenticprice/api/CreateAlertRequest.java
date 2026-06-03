package com.agenticprice.api;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateAlertRequest {
    private String productQuery;
    private BigDecimal thresholdPrice;
    private String email;
}