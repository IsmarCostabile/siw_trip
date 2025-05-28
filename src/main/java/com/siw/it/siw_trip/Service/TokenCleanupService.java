package com.siw.it.siw_trip.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TokenCleanupService {

    @Autowired
    private CredentialsService credentialsService;

    // Run every day at 2:00 AM to clean up expired tokens
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        credentialsService.cleanupExpiredTokens();
    }
}
