package org.gorpipe.gor.driver.linkfile;

/**
 * Link file content held in a cache, stamped with when the read that produced it started.
 *
 * <p>The stamp is what lets a save retire entries in caches it has no handle on.  Link content is
 * cached per session, and {@code GorSession.currentSession} is an InheritableThreadLocal that nothing
 * clears, so a read and a save of the same link file can legitimately land on different sessions: the
 * save can only invalidate the cache of the session bound to its own thread.  Rather than reach every
 * cache, a save records the time of the write and any entry stamped before it is dropped on lookup.
 *
 * <p>Stamped at the <i>start</i> of the read on purpose.  A read that opened its stream before the save
 * replaced the file holds the pre-write content even though it finished afterwards, so it is the start
 * of the read, not the end, that says whether the content can be trusted.
 *
 * @param content the link file content
 * @param readStartedNanos {@link System#nanoTime()} taken before the read that produced the content
 */
public record CachedLinkContent(String content, long readStartedNanos) {
}
