package enums;

public enum FlatType {
    TWO_ROOM("2-Room"), // Store display name
    THREE_ROOM("3-Room");
    // Add FOUR_ROOM, FIVE_ROOM etc. if needed in future

    private final String displayName;

    FlatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Helper to find enum from display name (useful for input/output)
    public static FlatType fromDisplayName(String text) {
        for (FlatType b : FlatType.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        // Handle cases like "2room", "2 room" if needed
        if ("2room".equalsIgnoreCase(text) || "2 room".equalsIgnoreCase(text)) return TWO_ROOM;
        if ("3room".equalsIgnoreCase(text) || "3 room".equalsIgnoreCase(text)) return THREE_ROOM;

        return null; // Or throw IllegalArgumentException
    }

     @Override
    public String toString() {
        return displayName; // Default toString to display name
    }
}
