package com.roxiun.mellow.api.coral;

public class CoralTag {

    private final String tagType;
    private final String reason;
    private final long addedOn;
    private final boolean hideUsername;
    private final Long addedBy;
    private final String addedByUsername;
    private final Long expiresAt;

    public CoralTag(
        String tagType,
        String reason,
        long addedOn,
        boolean hideUsername,
        Long addedBy,
        String addedByUsername,
        Long expiresAt
    ) {
        this.tagType = tagType;
        this.reason = reason;
        this.addedOn = addedOn;
        this.hideUsername = hideUsername;
        this.addedBy = addedBy;
        this.addedByUsername = addedByUsername;
        this.expiresAt = expiresAt;
    }

    /**
     * Retained for existing formatting code. Coral names this field tag_type.
     */
    public String getType() {
        return tagType;
    }

    public String getTagType() {
        return tagType;
    }

    public String getReason() {
        return reason;
    }

    public long getAddedOn() {
        return addedOn;
    }

    public boolean isHideUsername() {
        return hideUsername;
    }

    public Long getAddedBy() {
        return addedBy;
    }

    public String getAddedByUsername() {
        return addedByUsername;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
