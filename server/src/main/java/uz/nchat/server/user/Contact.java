package uz.nchat.server.user;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    @Column(name = "contact_user_id", nullable = false)
    private Long contactUserId;
    private String alias;
    @Column(nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public Long getContactUserId() { return contactUserId; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public LocalDate getCreatedAt() { return createdAt; }
}
