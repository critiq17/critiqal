package org.critiqal.domain.media.storage;

/**
 * Defines storage operations for media assets.
 * Implementations upload files and remove them by key or URL.
 */
public interface MediaStorage {
    public String upload(String key, byte[] data, String contentType);

    public void delete(String key);
    public void deleteByUrl(String url);
}
