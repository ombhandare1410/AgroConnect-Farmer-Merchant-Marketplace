package com.agroconnect.dto;

/** Simple platform counts, all calculated from the database. */
public record AdminSummary(long farmers, long merchants, long activeDeals, long completedDeals, long openComplaints) { }
