package uz.nchat.server.user;

public record UserDTO(
    Long id,
    String phoneNumber,
    String username,
    String displayName,
    String bio,
    String avatarUrl
) {

}
