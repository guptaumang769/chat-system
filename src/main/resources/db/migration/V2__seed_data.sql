-- Demo seed so the app is immediately explorable: three users, one 1:1 conversation
-- (Alice + Bob) and one group (Alice + Bob + Carol), plus a couple of messages.
-- Timestamps use now() to satisfy the NOT NULL audit / created_at columns.

INSERT INTO users (id, username, display_name, created_at, updated_at) VALUES
    (1, 'alice', 'Alice', now(), now()),
    (2, 'bob',   'Bob',   now(), now()),
    (3, 'carol', 'Carol', now(), now());

-- Conversation 1: one-to-one between Alice and Bob.
INSERT INTO conversations (id, type, name, created_at, updated_at) VALUES
    (1, 'ONE_TO_ONE', NULL, now(), now());
INSERT INTO conversation_members (conversation_id, user_id, created_at, updated_at) VALUES
    (1, 1, now(), now()),
    (1, 2, now(), now());

-- Conversation 2: group with all three users.
INSERT INTO conversations (id, type, name, created_at, updated_at) VALUES
    (2, 'GROUP', 'Weekend Plans', now(), now());
INSERT INTO conversation_members (conversation_id, user_id, created_at, updated_at) VALUES
    (2, 1, now(), now()),
    (2, 2, now(), now()),
    (2, 3, now(), now());

-- A couple of seed messages in the 1:1 so history reads return something.
INSERT INTO messages (conversation_id, sender_id, content, status, created_at) VALUES
    (1, 1, 'Hey Bob!',        'READ', now()),
    (1, 2, 'Hi Alice, whats up?', 'DELIVERED', now());

-- Keep the identity sequences ahead of the explicit ids inserted above.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('conversations', 'id'), (SELECT MAX(id) FROM conversations));
SELECT setval(pg_get_serial_sequence('conversation_members', 'id'), (SELECT MAX(id) FROM conversation_members));
SELECT setval(pg_get_serial_sequence('messages', 'id'), (SELECT MAX(id) FROM messages));
