package com.tripplanner.dto.response;

import java.math.BigDecimal;

public class ExpenseParticipantDto {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal shareAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
}
