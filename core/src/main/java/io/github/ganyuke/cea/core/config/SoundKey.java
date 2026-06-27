package io.github.ganyuke.cea.core.config;

public enum SoundKey {
    PORTAL_OPEN("portal_open"),
    PORTAL_CLOSE("portal_close"),
    PLAYER_ENTER("player_enter"),
    PLAYER_LEAVE("player_leave");
    private final String path;

    SoundKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
