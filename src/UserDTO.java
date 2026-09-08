/**
 * Session M3 Part A / B: replace this empty record header with the
 * fields Copilot suggests once you re-trigger ghost text after
 * opening User.java in a second tab. Then add a static
 * {@code fromUser(User)} mapper as described in Part B.
 */
public record UserDTO(String id, String email, String name, boolean active) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(
            String.valueOf(user.getId()),
            user.getEmail(),
            user.getName(),
            user.isActive()
        );
    }
}

public static UserDTO fromUser(User u) {
    return new UserDTO(
        String.valueOf(u.getId()),
        u.getEmail(),
        u.getName(),
        u.isActive()
    );
}
