package com.energytrade.logcollector.controller;

import com.energytrade.logcollector.model.LogEntry;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/log")
public class LogController {

    private final List<LogEntry> logs = Collections.synchronizedList(new ArrayList<>());

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void receiveLog(@RequestBody LogEntry entry) {
        logs.add(entry);
        System.out.printf("[LOG - %s] [%s] [%s] [%s]: %s%n",
        		entry.getTimestamp(),
        		entry.getLevel(),
        		entry.getService(),
        		entry.getSource(),
        		entry.getMessage()
        	);
    }

    @GetMapping("/view")
    public List<LogEntry> viewLogs() {
        return logs;
    }
}