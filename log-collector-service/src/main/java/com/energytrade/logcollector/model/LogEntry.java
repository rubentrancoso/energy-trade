package com.energytrade.logcollector.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private String timestamp;
    private String level;
    private String source;
    private String message;
    private String service;
}