package org.acme.domain.post_photo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class PostPhotoRepository implements PanacheRepository<PostPhoto> {

    public List<PostPhoto> findByPost(Long postId) {
        return find("post.id = ?1 ORDER BY position ASC", postId).list();
    }

    public long countByPost(Long postId) {
        return count("post.id = ?1", postId);
    }

    @Transactional
    public void deleteByPost(Long postId) {
        delete("post.id = ?1", postId);
    }
}
