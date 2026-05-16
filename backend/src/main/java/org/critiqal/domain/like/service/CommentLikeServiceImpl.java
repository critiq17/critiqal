package org.critiqal.domain.like.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.comment.repository.CommentRepository;
import org.critiqal.domain.like.LikeResult;
import org.critiqal.domain.like.repository.CommentLikeRepository;
import org.critiqal.domain.shared.exception.NotFoundException;

import java.util.UUID;

@ApplicationScoped
public class CommentLikeServiceImpl implements LikeService {

    private final CommentLikeRepository repo;
    private final CommentRepository commentRepo;

    public CommentLikeServiceImpl(CommentLikeRepository repo, CommentRepository commentRepo) {
        this.repo = repo;
        this.commentRepo = commentRepo;
    }

    @Override
    @Transactional
    public LikeResult toggle(UUID commentId, UUID userId) {
        commentRepo.findByIdOptional(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        boolean wasLiked = repo.exists(commentId, userId);
        if (wasLiked) {
            repo.remove(commentId, userId);
        } else {
            repo.save(commentId, userId);
        }

        long newCount = repo.count(commentId);
        return new LikeResult(!wasLiked, newCount);
    }

    @Override
    public long count(UUID commentId) {
        return repo.count(commentId);
    }

    @Override
    public boolean isLiked(UUID commentId, UUID userId) {
        return repo.exists(commentId, userId);
    }
}
