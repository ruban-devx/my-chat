package com.project.mychat;

import com.project.mychat.config.MyChatProperties;
import com.project.mychat.domain.Conversation;
import com.project.mychat.repository.ConversationRepository;
import com.project.mychat.repository.MessageRepository;
import com.project.mychat.repository.ParticipantRepository;
import com.project.mychat.service.ConversationService;
import com.project.mychat.service.MessageService;
import com.project.mychat.web.ChatExceptionHandler;
import com.project.mychat.web.ConversationController;
import com.project.mychat.web.MessageController;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers the chat starter's domain entities, JPA repositories, and services independently
 * of the host application's own base packages — this starter lives outside whatever package
 * the host app scans, so it must declare its own entity/repository locations explicitly.
 */
@AutoConfiguration(before = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@ConditionalOnProperty(prefix = "my-chat", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MyChatProperties.class)
@EntityScan(basePackageClasses = Conversation.class)
@EnableJpaRepositories(basePackageClasses = ConversationRepository.class)
public class MyChatAutoConfiguration {

    /**
     * A dedicated Flyway instance for chat's own tables, kept separate from the host app's
     * migrations via its own locations and schema history table so the two never collide.
     * Spring Boot makes {@code entityManagerFactory} beans depend on any bean of type
     * {@link Flyway} (not just the default one), so this still runs before Hibernate touches
     * the schema even though it isn't the app's primary Flyway bean.
     */
    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(prefix = "my-chat.migration", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "myChatFlyway")
    public Flyway myChatFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/mychat")
                .table("flyway_schema_history_mychat")
                .load();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationService conversationService(
            ConversationRepository conversationRepository, ParticipantRepository participantRepository) {
        return new ConversationService(conversationRepository, participantRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageService messageService(
            MessageRepository messageRepository, ConversationService conversationService, ApplicationEventPublisher eventPublisher) {
        return new MessageService(messageRepository, conversationService, eventPublisher);
    }

    @Bean
    @ConditionalOnProperty(prefix = "my-chat.api", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ConversationController conversationController(ConversationService conversationService) {
        return new ConversationController(conversationService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "my-chat.api", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MessageController messageController(MessageService messageService) {
        return new MessageController(messageService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "my-chat.api", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ChatExceptionHandler chatExceptionHandler() {
        return new ChatExceptionHandler();
    }
}
