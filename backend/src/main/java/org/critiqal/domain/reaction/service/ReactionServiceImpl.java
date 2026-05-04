package org.critiqal.domain.reaction.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;
import org.critiqal.domain.reaction.repository.ReactionRepository;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.user.service.UserService;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactRepo;
    private final PostService postService;
    private final UserService userService;

    public ReactionServiceImpl(ReactionRepository reactRepo,
                               PostService postService,
                               UserService userService) {
        this.reactRepo = reactRepo;
        this.postService = postService;
        this.userService = userService;
    }

    @Override
    public Map<ReactionType, Long> getReactions(Long postId) {
        return reactRepo.countByPost(postId);
    }

    @Override
    public Optional<ReactionType> getMyReaction(Long postId, Long userId) {
        return reactRepo.findByPostAndUser(postId, userId)
                .map(r -> r.type);
    }

    @Override
    @Transactional
    public Reaction react(Long userId, Long postId, ReactionType type) {
        var user = userService.getById(userId);
        var post = postService.getById(postId);

        var existing = reactRepo.findByPostAndUser(postId, userId);
        if (existing.isPresent()) {
            var reaction = existing.get();
            reaction.type = type;
            return reaction;
        }

        var reaction = new Reaction();
        reaction.user = user;
        reaction.post = post;
        reaction.type = type;
        return reactRepo.save(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(Long userId, Long postId) {
        reactRepo.deleteByPostAndUser(postId, userId);
    }
}
