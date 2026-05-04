package org.critiqal.domain.post_photo.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post_photo.PostPhoto;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostPhotoRepositoryImpl implements PostPhotoRepository, PanacheRepository<PostPhoto> {

    @Override
    public List<PostPhoto> findByPost(Long postId) {
        return find("post.id = ?1 ORDER BY position ASC", postId).list();
    }

    @Override
    public long countByPost(Long postId) {
        return count("post.id = ?1", postId);
    }

    @Override
    public Optional<PostPhoto> findByIdOptional(Long photoId) {
        return find("id", photoId).firstResultOptional();
    }

    @Override
    @Transactional
    public PostPhoto save(PostPhoto photo) {
        persist(photo);
        return photo;
    }

    @Override
    @Transactional
    public void delete(PostPhoto photo) {
        delete("id", photo.id);
    }

    @Override
    @Transactional
    public void deleteByPost(Long postId) {
        delete("post.id = ?1", postId);
    }
}
