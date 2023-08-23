package healthy.lifestyle.backend.exception;

public enum ErrorMessage {
    TITLE_DUPLICATE("Title Duplicate"),
    INVALID_SYMBOLS("Invalid symbols"),
    INVALID_NESTED_OBJECT("Invalid nested object"),
    ALREADY_EXISTS("Already exists"),
    SERVER_ERROR("Server error"),
    INVALID_TOKEN("Invalid token"),
    AUTHENTICATION_ERROR("Authentication error"),
    USER_NOT_FOUND("User not found");

    private final String name;

    ErrorMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
