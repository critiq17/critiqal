package org.critiqal.domain.reaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.PostService;
import org.critiqal.domain.user.UserService;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ReactionService {

    private final ReactionRepository reactRepo;
    private final PostService postService;
    private final UserService userService;

    public ReactionService(ReactionRepository reactRepo,
                           PostService postService,
                           UserService userService) {
        this.reactRepo = reactRepo;
        this.postService = postService;
        this.userService = userService;
    }

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
