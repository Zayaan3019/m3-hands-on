public record UserDTO(String id, String email, String name) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(user.id(), user.email(), user.name());
    }
}


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
