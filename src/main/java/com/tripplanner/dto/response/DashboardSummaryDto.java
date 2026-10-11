package com.tripplanner.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class DashboardSummaryDto {
    private int tripCount;
    private int upcomingTripCount;
    private BigDecimal totalExpenditure;
    private List<TripDto> recentTrips;
    private List<ExpenseDto> recentExpenses;

    public int getTripCount() { return tripCount; }
    public void setTripCount(int tripCount) { this.tripCount = tripCount; }
    public int getUpcomingTripCount() { return upcomingTripCount; }
    public void setUpcomingTripCount(int upcomingTripCount) { this.upcomingTripCount = upcomingTripCount; }
    public BigDecimal getTotalExpenditure() { return totalExpenditure; }
    public void setTotalExpenditure(BigDecimal totalExpenditure) { this.totalExpenditure = totalExpenditure; }
    public List<TripDto> getRecentTrips() { return recentTrips; }
    public void setRecentTrips(List<TripDto> recentTrips) { this.recentTrips = recentTrips; }
    public List<ExpenseDto> getRecentExpenses() { return recentExpenses; }
    public void setRecentExpenses(List<ExpenseDto> recentExpenses) { this.recentExpenses = recentExpenses; }
}
