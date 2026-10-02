package com.project.mychat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Optional link from a {@link Conversation} to a product-specific object (ticket, order,
 * invoice, ...), without chat ever knowing what that object means. Both fields are null
 * when a conversation has no product association.
 */
@Embeddable
@Getter
@Setter
public class ExternalReference {

    @Column(name = "external_reference_type", length = 100)
    private String type;

    @Column(name = "external_reference_id", length = 100)
    private String id;
}
