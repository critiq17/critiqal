package org.acme.domain.post;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.domain.user.User;

import java.util.List;

@ApplicationScoped
public class PostRepository implements PanacheRepository<Post> {

    public List<Post> findLastest() {
        return find("status = ?1 ORDER BY createdAt DESC", PostStatus.PUBLISHED)
                .page(0, 50)
                .list();
    }

    public List<Post> findByAuthor(Long authorId) {
        return find("author.id = ?1 AND status = ?2 ORDER BY createdAt DESC",
                authorId, PostStatus.PUBLISHED)
                .page(0, 50)
                .list();
    }

    public List<Post> search(String query) {
        return find("LOWER(content) LIKE ?1 AND status = ?2 ORDER BY createdAt DESC"
                    "%" + query.toLowerCase() + "%", PostStatus.PUBLISHED)
                .page(0, 50)
                .list();
    }

    @Transactional
    public Post createPost(User author, String content, String photoUrl) {
        var post = new Post();
        post.author = author;
        post.content = content;
        post.photoUrl = photoUrl;
        persist(post);
        return post;
    }

    @Transactional
    public void incrementViews(Long postId) {
        update("viewCount = viewCount + 1 WHERE id = ?1", postId);
    }
}
