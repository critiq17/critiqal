package org.critiqal.domain.like.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.LikeResult;
import org.critiqal.domain.like.repository.PostLikeRepository;
import org.critiqal.domain.post.repository.PostRepository;
import org.critiqal.domain.post.service.PostService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class PostLikeServiceImpl implements LikeService {

    private final PostLikeRepository repo;
    private final PostService postService;
    private final PostRepository postRepo;

    public PostLikeServiceImpl(PostLikeRepository repo, PostService postService,
                               PostRepository postRepo) {
        this.repo = repo;
        this.postService = postService;
        this.postRepo = postRepo;
    }

    @Override
    @Transactional
    public LikeResult toggle(UUID postId, UUID userId) {
        postService.getById(postId);

        boolean wasLiked = repo.exists(postId, userId);
        if (wasLiked) {
            repo.remove(postId, userId);
            postRepo.decrementLikeCount(postId);
        } else {
            repo.save(postId, userId);
            postRepo.incrementLikeCount(postId);
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

    public Map<UUID, Long> countByPostIds(List<UUID> postIds) {
        return repo.countByPostIds(postIds);
    }

    public Set<UUID> likedPostIds(UUID userId, List<UUID> postIds) {
        return repo.likedPostIds(userId, postIds);
    }
}
