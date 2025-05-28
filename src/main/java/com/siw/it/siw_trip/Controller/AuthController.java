package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Credentials;
import com.siw.it.siw_trip.Service.CredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private CredentialsService credentialsService;

    @GetMapping("/credentials")
    public ResponseEntity<List<Credentials>> getAllCredentials() {
        List<Credentials> credentials = credentialsService.findAll();
        return ResponseEntity.ok(credentials);
    }

    @GetMapping("/credentials/{id}")
    public ResponseEntity<Credentials> getCredentialsById(@PathVariable Long id) {
        Optional<Credentials> credentials = credentialsService.findById(id);
        return credentials.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credentials/username/{username}")
    public ResponseEntity<Credentials> getCredentialsByUsername(@PathVariable String username) {
        Optional<Credentials> credentials = credentialsService.findByUsername(username);
        return credentials.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credentials/username/{username}/with-user")
    public ResponseEntity<Credentials> getCredentialsByUsernameWithUser(@PathVariable String username) {
        Optional<Credentials> credentials = credentialsService.findByUsernameWithUser(username);
        return credentials.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credentials/user/{userId}")
    public ResponseEntity<Credentials> getCredentialsByUserId(@PathVariable Long userId) {
        Optional<Credentials> credentials = credentialsService.findByUserId(userId);
        return credentials.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credentials/expired")
    public ResponseEntity<List<Credentials>> getExpiredPasswordCredentials(@RequestParam int daysAgo) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        List<Credentials> credentials = credentialsService.findByLastPasswordChangeBefore(cutoffDate);
        return ResponseEntity.ok(credentials);
    }

    @PostMapping("/register")
    public ResponseEntity<Credentials> registerCredentials(@RequestBody Credentials credentials) {
        if (credentialsService.existsByUsername(credentials.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Credentials savedCredentials = credentialsService.saveWithEncodedPassword(credentials);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCredentials);
    }

    @PostMapping("/login")
    public ResponseEntity<Credentials> login(@RequestBody LoginRequest loginRequest) {
        Optional<Credentials> credentials = credentialsService.findByUsername(loginRequest.getUsername());
        
        if (credentials.isPresent() && credentialsService.verifyPassword(loginRequest.getPassword(), credentials.get().getPassword())) {
            return ResponseEntity.ok(credentials.get());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PutMapping("/credentials/{id}")
    public ResponseEntity<Credentials> updateCredentials(@PathVariable Long id, @RequestBody Credentials credentials) {
        if (!credentialsService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        credentials.setId(id);
        Credentials updatedCredentials = credentialsService.save(credentials);
        return ResponseEntity.ok(updatedCredentials);
    }

    @PutMapping("/change-password/{id}")
    public ResponseEntity<Credentials> changePassword(
            @PathVariable Long id, 
            @RequestBody PasswordChangeRequest passwordChangeRequest) {
        Optional<Credentials> credentialsOpt = credentialsService.findById(id);
        
        if (!credentialsOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Credentials credentials = credentialsOpt.get();
        if (!credentialsService.verifyPassword(passwordChangeRequest.getCurrentPassword(), credentials.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Credentials updatedCredentials = credentialsService.updatePassword(id, passwordChangeRequest.getNewPassword());
        return ResponseEntity.ok(updatedCredentials);
    }

    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<Void> deleteCredentials(@PathVariable Long id) {
        if (!credentialsService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        credentialsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<Boolean> checkUsernameExists(@PathVariable String username) {
        boolean exists = credentialsService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    // Inner classes for request bodies
    public static class LoginRequest {
        private String username;
        private String password;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
        
        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
