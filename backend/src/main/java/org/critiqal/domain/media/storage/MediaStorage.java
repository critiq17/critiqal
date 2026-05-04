package org.critiqal.domain.media.storage;

public interface MediaStorage {
    public String upload(String key, byte[] data, String contentType);

    public void delete(String key);
    public void deleteByUrl(String url);
}
