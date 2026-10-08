# Chat System — Diagrams

Mermaid diagrams (render natively on GitHub). Generated from the actual entities under
`src/main/java/com/umang/chat/model/entity/` and the service/ws layers.

- [1. High-Level Design (HLD)](#1-high-level-design-hld)
- [2. Message-Send Sequence](#2-message-send-sequence)
- [3. Message Delivery State Machine](#3-message-delivery-state-machine)
- [4. UML Class / ER Diagram](#4-uml-class--er-diagram)

---

## 1. High-Level Design (HLD)

```mermaid
flowchart TB
    subgraph Clients
      A[Client A]
      B[Client B]
    end

    A & B -->|STOMP / WebSocket| LB[Load Balancer]
    LB --> N1[Connection server node-1<br/>Spring Boot · stateless]
    LB --> N2[Connection server node-2<br/>Spring Boot · stateless]

    subgraph Node[Per-node internals]
      Ctrl[ChatController<br/>MessageMapping] --> Svc[ChatService<br/>persist + outbox]
      Interceptor[PresenceChannelInterceptor] --> Presence[PresenceService]
      Consumer[MessageFanoutConsumer] --> Delivery[WebSocketDeliveryService]
      Delivery --> Registry[LocalSessionRegistry]
    end

    N1 --- Node
    N2 --- Node

    Svc -->|message + event in 1 tx| PG[(PostgreSQL<br/>messages · conversations · outbox)]
    Poller[OutboxPoller @Scheduled] -->|relay unpublished| Kafka{{Kafka<br/>chat-messages<br/>keyed by conversation}}
    PG --> Poller
    Kafka --> Consumer
    Kafka -. on failure .-> DLT{{chat-messages.DLT}}

    Delivery -->|recipient on this node| LocalPush[Local STOMP push]
    Delivery -->|recipient on another node| RedisPS[(Redis pub/sub<br/>chat.delivery)]
    RedisPS --> Consumer2[Other node's subscriber]

    Presence -->|presence:userId TTL · lastseen:userId| RedisP[(Redis<br/>presence)]

    N1 & N2 -.metrics.-> Prom[(Prometheus)]
```

---

## 2. Message-Send Sequence

```mermaid
sequenceDiagram
    participant A as Client A (sender)
    participant WS as Connection server
    participant DB as PostgreSQL
    participant K as Kafka
    participant C as FanoutConsumer
    participant R as Redis (presence + pub/sub)
    participant B as Client B (recipient)

    A->>WS: SEND /app/chat.send {conversationId, content}
    WS->>DB: INSERT message (status=SENT) + INSERT outbox_event  (one tx)
    Note over WS,DB: transactional outbox — no dual-write to Kafka
    WS-->>A: (WS thread returns; ack is async)

    loop OutboxPoller @Scheduled
        WS->>DB: read unpublished outbox rows
        WS->>K: publish MessageEvent (key = conversationId)
        WS->>DB: mark row published
    end

    K->>C: consume MessageEvent
    C->>DB: recipientsOf(conversation) = members - sender
    C->>R: isOnline(B)?
    alt B online on same node
        C->>B: push /user/queue/messages
    else B online on another node
        C->>R: publish CrossNodeEnvelope (chat.delivery)
        R->>B: owning node pushes /user/queue/messages
    else B offline
        Note over C,DB: leave status=SENT; B pulls it on reconnect
    end
    C->>DB: status SENT -> DELIVERED
    C->>A: push /user/queue/receipts (DELIVERED)

    B->>WS: SEND /app/chat.ack {messageId, status=READ}
    WS->>DB: status DELIVERED -> READ
    WS->>A: push /user/queue/receipts (READ)
```

---

## 3. Message Delivery State Machine

```mermaid
stateDiagram-v2
    [*] --> SENT : sendMessage()<br/>persisted, single grey tick
    SENT --> DELIVERED : fanned out to an online device<br/>double grey tick
    DELIVERED --> READ : recipient opens chat (/app/chat.ack)<br/>double blue tick
    SENT --> READ : recipient reads before a DELIVERED ack lands<br/>(forward jump allowed)
    READ --> [*]

    note right of SENT
        Offline recipient: stays SENT
        until pulled on reconnect
    end note
```

---

## 4. UML Class / ER Diagram

```mermaid
classDiagram
    class User {
      +Long id
      +String username
      +String displayName
    }
    class Conversation {
      +Long id
      +ConversationType type
      +String name
    }
    class ConversationMember {
      +Long id
      +Long conversationId
      +Long userId
    }
    class Message {
      +Long id
      +Long conversationId
      +Long senderId
      +String content
      +MessageStatus status
      +Instant createdAt
    }
    class OutboxEvent {
      +Long id
      +String aggregateId
      +String eventType
      +String payload
      +boolean published
    }
    class ConversationType {
      <<enumeration>>
      ONE_TO_ONE
      GROUP
    }
    class MessageStatus {
      <<enumeration>>
      SENT
      DELIVERED
      READ
    }

    Conversation "1" o-- "many" ConversationMember : has members
    User "1" o-- "many" ConversationMember : belongs to
    Conversation "1" o-- "many" Message : contains
    User "1" o-- "many" Message : sends
    Conversation ..> ConversationType
    Message ..> MessageStatus
```

```mermaid
erDiagram
    USERS ||--o{ CONVERSATION_MEMBERS : joins
    CONVERSATIONS ||--o{ CONVERSATION_MEMBERS : has
    CONVERSATIONS ||--o{ MESSAGES : contains
    USERS ||--o{ MESSAGES : sends

    USERS {
      bigint id PK
      varchar username UK
      varchar display_name
    }
    CONVERSATIONS {
      bigint id PK
      varchar type "ONE_TO_ONE | GROUP"
      varchar name
    }
    CONVERSATION_MEMBERS {
      bigint id PK
      bigint conversation_id FK
      bigint user_id FK
    }
    MESSAGES {
      bigint id PK
      bigint conversation_id FK
      bigint sender_id FK
      text content
      varchar status "SENT | DELIVERED | READ"
      timestamptz created_at
    }
    OUTBOX_EVENTS {
      bigint id PK
      varchar aggregate_id
      varchar event_type
      text payload
      boolean published
    }
```
