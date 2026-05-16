package org.critiqal.domain.like.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.LikeResult;
import org.critiqal.domain.like.repository.PostLikeRepository;
import org.critiqal.domain.like.repository.PostLikeRepositoryImpl;
import org.critiqal.domain.post.service.PostService;

import java.util.UUID;

@ApplicationScoped
public class PostLikeServiceImpl implements LikeService {

    private final PostLikeRepository repo;
    private final PostService postService;

    public PostLikeServiceImpl(PostLikeRepository repo, PostService postService) {
        this.repo = repo;
        this.postService = postService;
    }

    @Override
    @Transactional
    public LikeResult toggle(UUID postId, UUID userId) {
        postService.getById(postId);

        boolean wasLiked = repo.exists(postId, userId);
        if (wasLiked) {
            repo.remove(postId, userId);
        } else {
            repo.save(postId, userId);
        }

        long newCount = repo.count(postId);
        return new LikeResult(!wasLiked, newCount);
    }

    @Override
    public long count(UUID postId) {
        return repo.count(postId);
    }

    @Override
    public boolean isLiked(UUID postId, UUID userId) {
        return repo.exists(postId, userId);
    }
}
