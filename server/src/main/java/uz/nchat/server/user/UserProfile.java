package uz.nchat.server.user;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String displayName;

    private String bio;

    private String avatarUrl;

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    protected UserProfile() {}

    public UserProfile(String phoneNumber, String displayName) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public LocalDate getCreatedAt() { return createdAt; }
}
