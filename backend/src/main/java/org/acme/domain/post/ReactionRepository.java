package org.acme.domain.post;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.transaction.Transactional;
import org.acme.domain.user.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReactionRepository implements PanacheRepository<Reaction> {

    public List<Reaction> findByPost(Long postId) {
        return find("post.id = ?1", postId).list();
    }

    public Optional<Reaction> findByPostAndUser(Long postId, Long userId) {
        return find("post.id = ?1 AND user.id = ?2", postId, userId)
                .firstResultOptional();
    }

    public Map<ReactionType, Long> countByPost(Long postId) {
        return find("post.id = ?1", postId).stream()
                .collect(Collectors.groupingBy(r -> r.type, Collectors.counting()));
    }

    @Transactional
    public Reaction react(User user, Post post, ReactionType type) {
        var existing = findByPostAndUser(post.id, user.id);
        if (existing.isPresent()) {
            var reaction = existing.get();
            reaction.type = type;
            return reaction;
        }

        var reaction = new Reaction();
        reaction.user = user;
        reaction.post = post;
        reaction.type = type;
        persist(reaction);
        return reaction;
    }

    @Transactional
    public void removeReaction(Long postId, Long userId) {
        delete("post.id = ?1 AND user.id = ?2", postId, userId);
    }
}
