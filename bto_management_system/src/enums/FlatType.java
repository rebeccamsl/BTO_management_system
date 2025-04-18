package enums;

public enum FlatType {
    TWO_ROOM("2-Room"), 
    THREE_ROOM("3-Room");
    
    private final String displayName;

    FlatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Helper to find enum from display name 
    public static FlatType fromDisplayName(String text) {
        for (FlatType b : FlatType.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        // Handle cases like "2room", "2 room" 
        if ("2room".equalsIgnoreCase(text) || "2 room".equalsIgnoreCase(text)) return TWO_ROOM;
        if ("3room".equalsIgnoreCase(text) || "3 room".equalsIgnoreCase(text)) return THREE_ROOM;

        return null; // 
    }

     @Override
    public String toString() {
        return displayName; 
    }
}
