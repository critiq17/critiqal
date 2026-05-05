package org.critiqal.domain.post_view.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.post_view.PostView;
import org.critiqal.domain.post_view.PostViewId;

import java.util.Optional;

/**
 * Panache-backed implementation of {@link PostViewRepository}.
 * Persists post view records keyed by post and user.
 */
@ApplicationScoped
public class PostViewRepositoryImpl implements PostViewRepository, PanacheRepositoryBase<PostView, PostViewId> {

    @Override
    public Optional<PostView> findByIdOptional(PostViewId id) {
        return Optional.ofNullable(findById(id));
    }

    @Override
    public PostView save(PostView postView) {
        persist(postView);
        return postView;
    }
}
