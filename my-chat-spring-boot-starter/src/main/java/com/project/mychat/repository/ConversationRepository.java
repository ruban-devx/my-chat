package com.project.mychat.repository;

import com.project.mychat.domain.Conversation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            select c from Conversation c
            where c.id in (
                select p.conversationId from Participant p
                where p.externalUserId = :externalUserId and p.leftAt is null
            )
            order by c.updatedAt desc
            """)
    List<Conversation> findActiveForParticipant(@Param("externalUserId") String externalUserId);
}
