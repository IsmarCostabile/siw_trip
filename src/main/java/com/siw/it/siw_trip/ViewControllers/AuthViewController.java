package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Credentials;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.UserRole;
import com.siw.it.siw_trip.Service.CredentialsService;
import com.siw.it.siw_trip.Service.UserService;
import com.siw.it.siw_trip.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;

import java.util.Optional;

@Controller
public class AuthViewController {

    @Autowired
    private EmailService emailService;
    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, 
                             @RequestParam String password, 
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        try {
            Optional<Credentials> credentialsOpt = credentialsService.findByUsernameWithUser(username);
            
            if (credentialsOpt.isPresent()) {
                Credentials credentials = credentialsOpt.get();
                
                // Use password encoder to verify password
                if (credentialsService.verifyPassword(password, credentials.getPassword())) {
                    // Login successful - store user in session
                    session.setAttribute("loggedInUser", credentials.getUser());
                    session.setAttribute("loggedInCredentials", credentials);
                    
                    redirectAttributes.addFlashAttribute("successMessage", "Login successful! Welcome back.");
                    return "redirect:/trips";
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid username or password.");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid username or password.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during login. Please try again.");
        }
        
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam String firstName,
                                    @RequestParam String lastName,
                                    @RequestParam String email,
                                    @RequestParam String username,
                                    @RequestParam String password,
                                    @RequestParam String confirmPassword,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Validation
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
                return "redirect:/register";
            }

            if (credentialsService.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Username already exists. Please choose a different one.");
                return "redirect:/register";
            }

            if (userService.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email already registered. Please use a different email.");
                return "redirect:/register";
            }

            // Create new user
            User newUser = new User(email, firstName, lastName, UserRole.USER);
            User savedUser = userService.save(newUser);

            // Create credentials with encrypted password
            Credentials newCredentials = new Credentials(username, password);
            newCredentials.setUser(savedUser);
            credentialsService.saveWithEncodedPassword(newCredentials);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! You can now log in.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during registration. Please try again.");
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, 
                                       RedirectAttributes redirectAttributes) {
        try {
            credentialsService.sendPasswordResetEmail(email);
            redirectAttributes.addFlashAttribute("successMessage", 
                "If an account with that email exists, a password reset link has been sent.");
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Error sending password reset email: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "An error occurred. Please try again.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        System.out.println("Processing password reset for token: " + token);
        
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }
        
        // Validate password length
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters long.");
            return "redirect:/reset-password?token=" + token;
        }
        
        try {
            boolean resetSuccessful = credentialsService.resetPassword(token, password);
            
            if (resetSuccessful) {
                System.out.println("Password reset successful for token: " + token);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Password reset successful! You can now log in with your new password.");
                return "redirect:/login";
            } else {
                System.out.println("Password reset failed - invalid or expired token: " + token);
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Invalid or expired reset token. Please request a new password reset link.");
                return "redirect:/login";
            }
        } catch (Exception e) {
            System.err.println("Error during password reset: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "An error occurred while resetting your password. Please try again.");
            return "redirect:/reset-password?token=" + token;
        }
    }
}
