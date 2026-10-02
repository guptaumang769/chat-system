package com.umang.chat.model.entity;

import com.umang.chat.model.enums.ConversationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A chat thread. {@code ONE_TO_ONE} has exactly two members; {@code GROUP} has many.
 * We deliberately model both with the same Conversation + ConversationMember pair so
 * fan-out logic is uniform: "send to every member except the sender".
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConversationType type;

    /** Group name; null for one-to-one conversations. */
    @Column(name = "name")
    private String name;
}
