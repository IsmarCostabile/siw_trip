package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Credentials;
import com.siw.it.siw_trip.Model.PasswordResetToken;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Repository.CredentialsRepository;
import com.siw.it.siw_trip.Repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialsService {

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    public List<Credentials> findAll() {
        return credentialsRepository.findAll();
    }
    
    public Optional<Credentials> findById(Long id) {
        return credentialsRepository.findById(id);
    }

    public Credentials save(Credentials credentials) {
        return credentialsRepository.save(credentials);
    }

    public void deleteById(Long id) {
        credentialsRepository.deleteById(id);
    }

    public Optional<Credentials> findByUsername(String username) {
        return credentialsRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return credentialsRepository.existsByUsername(username);
    }

    public Optional<Credentials> findByUsernameWithUser(String username) {
        return credentialsRepository.findByUsernameWithUser(username);
    }

    public List<Credentials> findByLastPasswordChangeBefore(LocalDateTime dateTime) {
        return credentialsRepository.findByLastPasswordChangeBefore(dateTime);
    }

    public Optional<Credentials> findByUserId(Long userId) {
        return credentialsRepository.findByUserId(userId);
    }

    public Credentials saveWithEncodedPassword(Credentials credentials) {
        // Encode the password before saving
        credentials.setPassword(passwordEncoder.encode(credentials.getPassword()));
        return credentialsRepository.save(credentials);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Credentials updatePassword(Long credentialsId, String newPassword) {
        Optional<Credentials> credentialsOpt = findById(credentialsId);
        if (credentialsOpt.isPresent()) {
            Credentials credentials = credentialsOpt.get();
            credentials.setPassword(passwordEncoder.encode(newPassword));
            return save(credentials);
        }
        throw new RuntimeException("Credentials not found with id: " + credentialsId);
    }

    @Transactional
    public void sendPasswordResetEmail(String email) {
        System.out.println("Attempting to send password reset email to: " + email);
        
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("User found: " + user.getId());
            
            Optional<Credentials> credentialsOpt = findByUserId(user.getId());
            
            if (credentialsOpt.isPresent()) {
                Credentials credentials = credentialsOpt.get();
                System.out.println("Credentials found for user: " + credentials.getId());
                
                // Delete any existing tokens for this user
                try {
                    passwordResetTokenRepository.deleteByCredentialsId(credentials.getId());
                    System.out.println("Deleted existing tokens for credentials: " + credentials.getId());
                } catch (Exception e) {
                    System.out.println("No existing tokens to delete or error: " + e.getMessage());
                }
                
                // Generate unique token
                String token = UUID.randomUUID().toString();
                System.out.println("Generated token: " + token);
                
                // Create reset token entity (expires in 24 hours)
                PasswordResetToken resetToken = new PasswordResetToken(
                    token, 
                    credentials, 
                    LocalDateTime.now().plusHours(24)
                );
                
                PasswordResetToken savedToken = passwordResetTokenRepository.save(resetToken);
                System.out.println("Saved reset token with ID: " + savedToken.getId());
                
                // Send email
                emailService.sendPasswordResetEmail(user.getEmail(), token);
                System.out.println("Email sent successfully to: " + user.getEmail());
            } else {
                System.out.println("No credentials found for user: " + user.getId());
            }
        } else {
            System.out.println("No user found with email: " + email);
        }
        // Note: We don't throw an exception if user doesn't exist for security reasons
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        System.out.println("Attempting to reset password with token: " + token);
        
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOpt.isPresent()) {
            PasswordResetToken resetToken = tokenOpt.get();
            System.out.println("Token found. Valid: " + resetToken.isValid() + 
                             ", Used: " + resetToken.isUsed() + 
                             ", Expired: " + resetToken.isExpired());
            
            if (resetToken.isValid()) {
                // Reset password
                Credentials credentials = resetToken.getCredentials();
                System.out.println("Resetting password for credentials ID: " + credentials.getId());
                
                String oldPasswordHash = credentials.getPassword();
                credentials.setPassword(passwordEncoder.encode(newPassword));
                save(credentials);
                
                System.out.println("Password updated. Old hash: " + oldPasswordHash.substring(0, 10) + "...");
                System.out.println("New hash: " + credentials.getPassword().substring(0, 10) + "...");
                
                // Mark token as used
                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);
                
                System.out.println("Token marked as used");
                return true;
            } else {
                System.out.println("Token is invalid: " + 
                                 (resetToken.isUsed() ? "already used" : "expired"));
            }
        } else {
            System.out.println("Token not found in database: " + token);
        }
        return false;
    }

    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    
     // Find a user by username using the credentials relationship
     public Optional<User> findUserByUsername(String username) {
        Optional<Credentials> credentials = findByUsernameWithUser(username);
        return credentials.map(Credentials::getUser);
    }


}
