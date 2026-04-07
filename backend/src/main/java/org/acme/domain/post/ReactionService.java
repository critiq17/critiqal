package org.acme.domain.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.user.UserService;

import java.lang.annotation.Inherited;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ReactionService {

    @Inject ReactionRepository reactRepo;
    @Inject PostService postService;
    @Inject UserService userService;

    public Map<ReactionType, Long> getReactions(Long postId) {
        return reactRepo.countByPost(postId);
    }

    public Optional<ReactionType> getMyReaction(Long postId, Long userId) {
        return reactRepo.findByPostAndUser(postId, userId)
                .map(r -> r.type);
    }

    @Transactional
    public Reaction react(Long userId, Long postId, ReactionType type) {
        var user = userService.getById(userId);
        var post = postService.getById(postId);
        return reactRepo.react(user, post, type);
    }

    @Transactional
    public void removeReact(Long userId, Long postId) {
        reactRepo.removeReaction(postId, userId);
    }
}
