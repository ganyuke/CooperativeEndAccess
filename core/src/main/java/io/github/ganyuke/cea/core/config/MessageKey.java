package io.github.ganyuke.cea.core.config;

public enum MessageKey {
    MAX_EYES_ERROR("max_eyes_error"),
    COMMITTED_NOTICE("committed_notice"),
    RESCIND_WARNING("rescind_warning"),
    DRAGON_DEFEAT_NOTICE("dragon_defeat_notice"),
    ACTIVE_ACTION_BAR("active_action_bar"),
    STABILIZED_ACTION_BAR("stabilized_action_bar"),
    WAITING_ACTION_BAR("waiting_action_bar"),
    NON_OWNER_RESCIND_WARNING("non_owner_rescind_warning");
    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}