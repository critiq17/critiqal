package org.critiqal.domain.post_view.repository;

import org.critiqal.domain.post_view.PostView;
import org.critiqal.domain.post_view.PostViewId;

import java.util.Optional;

/**
 * Defines persistence operations for post view records.
 * Supports lookup and storage by composite identifier.
 */
public interface PostViewRepository {

    Optional<PostView> findByIdOptional(PostViewId id);

    PostView save(PostView postView);
}
