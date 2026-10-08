package io.github.easychat.core;

/** Immutable event shared between the worker and the client render thread. */
public final class StatusHint {
    public final String text;
    public final long expiresAt;
    public final boolean chat;
    public StatusHint(String text, long now, boolean chat) {
        this.text = text; this.expiresAt = now + 5000; this.chat = chat;
    }
    public boolean visible(long now) { return now < expiresAt; }
}
