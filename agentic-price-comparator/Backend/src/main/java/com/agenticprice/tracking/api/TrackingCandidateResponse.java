package com.agenticprice.tracking.api;

import lombok.Data;

import java.util.UUID;

@Data
public class TrackingCandidateResponse {
    private UUID id;
    private String rawQuery;
    private boolean active;
}
