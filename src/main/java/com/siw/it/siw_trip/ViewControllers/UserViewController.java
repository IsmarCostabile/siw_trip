package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Credentials;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.UserRole;
import com.siw.it.siw_trip.Service.CredentialsService;
import com.siw.it.siw_trip.Service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class UserViewController {

    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_PROFILE = "redirect:/profile";
    private static final String REDIRECT_CHANGE_PASSWORD = "redirect:/profile/change-password";
    private static final String REDIRECT_EDIT_PROFILE = "redirect:/profile/edit";

    private final UserService userService;
    private final CredentialsService credentialsService;

    public UserViewController(UserService userService, CredentialsService credentialsService) {
        this.userService = userService;
        this.credentialsService = credentialsService;
    }

    @GetMapping
    public String showProfile(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to view your profile.");
            return REDIRECT_LOGIN;
        }

        model.addAttribute("user", loggedInUser);

        // If user is admin, add users management data
        if (loggedInUser.isAdmin()) {
            List<User> allUsers = userService.findAll();
            model.addAttribute("allUsers", allUsers);
        }

        return "profile/profile";
    }

    @GetMapping("/edit")
    public String showEditProfile(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to edit your profile.");
            return REDIRECT_LOGIN;
        }

        model.addAttribute("user", loggedInUser);
        return "profile/edit";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to update your profile.");
            return REDIRECT_LOGIN;
        }

        try {
            // Check if email is taken by another user
            Optional<User> existingUser = userService.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(loggedInUser.getId())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Email is already taken by another user.");
                return REDIRECT_EDIT_PROFILE;
            }

            // Update user information
            loggedInUser.setFirstName(firstName);
            loggedInUser.setLastName(lastName);
            loggedInUser.setEmail(email);

            User updatedUser = userService.save(loggedInUser);
            
            // Update session
            session.setAttribute(LOGGED_IN_USER, updatedUser);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Profile updated successfully!");
            return REDIRECT_PROFILE;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error updating profile. Please try again.");
            return REDIRECT_EDIT_PROFILE;
        }
    }

    @GetMapping("/change-password")
    public String showChangePassword(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to change your password.");
            return REDIRECT_LOGIN;
        }

        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to change your password.");
            return REDIRECT_LOGIN;
        }

        try {
            // Validate password confirmation
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Passwords do not match.");
                return REDIRECT_CHANGE_PASSWORD;
            }

            // Validate password length
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Password must be at least 6 characters long.");
                return REDIRECT_CHANGE_PASSWORD;
            }

            // Get user's credentials
            Optional<Credentials> credentialsOpt = credentialsService.findByUserId(loggedInUser.getId());
            if (!credentialsOpt.isPresent()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Credentials not found.");
                return REDIRECT_CHANGE_PASSWORD;
            }

            Credentials credentials = credentialsOpt.get();

            // Verify current password
            if (!credentialsService.verifyPassword(currentPassword, credentials.getPassword())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Current password is incorrect.");
                return REDIRECT_CHANGE_PASSWORD;
            }

            // Update password
            credentialsService.updatePassword(credentials.getId(), newPassword);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Password changed successfully!");
            return REDIRECT_PROFILE;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error changing password. Please try again.");
            return REDIRECT_CHANGE_PASSWORD;
        }
    }

    // Admin-only endpoint to search users via form submission
    @GetMapping("/users/search")
    public String searchUsersForm(@RequestParam(required = false) String query, 
                                 Model model, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to view your profile.");
            return REDIRECT_LOGIN;
        }
        
        if (!loggedInUser.isAdmin()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Access denied. Admin privileges required.");
            return REDIRECT_PROFILE;
        }

        // Add user to model
        model.addAttribute("user", loggedInUser);

        // Search for users
        List<User> allUsers;
        if (query != null && !query.trim().isEmpty()) {
            allUsers = userService.findByNameContaining(query.trim());
            model.addAttribute("searchQuery", query);
        } else {
            allUsers = userService.findAll();
        }
        
        model.addAttribute("allUsers", allUsers);
        return "profile/profile";
    }

    // Admin-only endpoint to promote user to admin
    @PostMapping("/users/{id}/promote")
    public String promoteUserToAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null || !loggedInUser.isAdmin()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Access denied. Admin privileges required.");
            return REDIRECT_PROFILE;
        }

        try {
            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User not found.");
                return REDIRECT_PROFILE;
            }

            User user = userOpt.get();
            if (user.isAdmin()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is already an admin.");
                return REDIRECT_PROFILE;
            }

            user.setRole(UserRole.ADMIN);
            userService.save(user);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "User " + user.getFullName() + " has been promoted to admin successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error promoting user. Please try again.");
        }

        return REDIRECT_PROFILE;
    }

    // Admin-only endpoint to demote admin to regular user
    @PostMapping("/users/{id}/demote")
    public String demoteAdminToUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null || !loggedInUser.isAdmin()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Access denied. Admin privileges required.");
            return REDIRECT_PROFILE;
        }

        try {
            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User not found.");
                return REDIRECT_PROFILE;
            }

            User user = userOpt.get();
            
            // Prevent self-demotion
            if (user.getId().equals(loggedInUser.getId())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You cannot demote yourself.");
                return REDIRECT_PROFILE;
            }

            if (!user.isAdmin()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is not an admin.");
                return REDIRECT_PROFILE;
            }

            user.setRole(UserRole.USER);
            userService.save(user);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "User " + user.getFullName() + " has been demoted to regular user successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error demoting user. Please try again.");
        }

        return REDIRECT_PROFILE;
    }

    /**
     * Display public user profile by username
     */
    @GetMapping("/{username}")
    public String showPublicProfile(@PathVariable String username, Model model, HttpSession session) {
        // Find user by username
        Optional<User> userOpt = credentialsService.findUserByUsername(username);
        
        if (userOpt.isEmpty()) {
            model.addAttribute(ERROR_MESSAGE, "User not found");
            return "error"; // Return to a generic error page or 404 page
        }
        
        User profileUser = userOpt.get();
        
        // Get current logged-in user (can be null for anonymous users)
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        
        // Check if viewing own profile
        boolean isOwnProfile = loggedInUser != null && loggedInUser.getId().equals(profileUser.getId());
        
        // Add user to model
        model.addAttribute("user", profileUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("public", true);
        
        // If it's the user's own profile, redirect to private profile page
        if (isOwnProfile) {
            return REDIRECT_PROFILE;
        }
        
        return "profile/public-profile";
    }
}
