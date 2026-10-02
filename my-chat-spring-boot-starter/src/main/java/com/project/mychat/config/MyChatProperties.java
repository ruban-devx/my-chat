package com.project.mychat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "my-chat")
public class MyChatProperties {

    /** Master switch for the whole starter. */
    private boolean enabled = true;

    private final Migration migration = new Migration();

    private final Api api = new Api();

    private final Realtime realtime = new Realtime();

    @Getter
    @Setter
    public static class Migration {

        /** Whether the starter runs its own Flyway migrations on startup. */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Api {

        /** Whether the starter registers its own REST controllers under /api/chat. */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Realtime {

        /** Whether the starter registers its own STOMP/WebSocket endpoint under /ws/chat. */
        private boolean enabled = true;
    }
}
