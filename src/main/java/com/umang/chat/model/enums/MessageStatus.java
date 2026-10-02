package com.umang.chat.model.enums;

/**
 * Delivery lifecycle of a message — the "ticks" a WhatsApp user sees.
 *
 * <ul>
 *   <li>{@code SENT} — persisted on the server (single grey tick). The sender's write
 *       succeeded but no recipient device has received it yet.</li>
 *   <li>{@code DELIVERED} — pushed to at least one recipient's device (double grey tick).</li>
 *   <li>{@code READ} — the recipient opened the conversation (double blue tick).</li>
 * </ul>
 *
 * Transitions only ever move forward: SENT → DELIVERED → READ.
 */
public enum MessageStatus {
    SENT,
    DELIVERED,
    READ
}
