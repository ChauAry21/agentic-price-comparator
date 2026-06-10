package com.agenticprice.tracking.api;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class TrackingCandidateResponse {
    private String query;
    private boolean autoSelected;
    private List<TrackingCandidate> candidates;

    @Data
    public static class TrackingCandidate {
        private String productKey;
        private String productName;
        private String sampleUrl;
        private List<String> retailers;
        private BigDecimal lowestPrice;
        private int resultCount;
    }
}
