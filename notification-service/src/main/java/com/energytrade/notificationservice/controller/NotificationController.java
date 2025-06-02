package com.energytrade.notificationservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.energytrade.notificationservice.model.Notification;

@RestController
@RequestMapping("/notify")
public class NotificationController {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public void sendNotification(@RequestBody Notification notification) {
    	logger.info("🔔 Notification sent to " + notification.getTarget() + ": " + notification.getMessage());
    }
}