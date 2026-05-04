package org.critiqal.domain.reaction.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReactionRepositoryImpl implements ReactionRepository, PanacheRepository<Reaction> {

    @Override
    public Optional<Reaction> findByPostAndUser(Long postId, Long userId) {
        return find("post.id = ?1 AND user.id = ?2", postId, userId)
                .firstResultOptional();
    }

    @Override
    public Map<ReactionType, Long> countByPost(Long postId) {
        return find("post.id = ?1", postId).stream()
                .collect(Collectors.groupingBy(r -> r.type, Collectors.counting()));
    }

    @Override
    @Transactional
    public Reaction save(Reaction reaction) {
        persist(reaction);
        return reaction;
    }

    @Override
    @Transactional
    public void deleteByPostAndUser(Long postId, Long userId) {
        delete("post.id = ?1 AND user.id = ?2", postId, userId);
    }
}
