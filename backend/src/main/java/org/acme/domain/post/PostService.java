package org.acme.domain.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.user.UserService;

import java.util.List;

@ApplicationScoped
public class PostService {

    @Inject PostRepository postRepo;
    @Inject
    UserService userService;

    @Inject
    Event<PostCreatedEvent> postCreatedEvent;

    @Transactional
    public Post createPost(Long authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post cannot be empty");
        }
        var author = userService.getById(authorId);
        var post = postRepo.createPost(author, content);

        postCreatedEvent.fireAsync(new PostCreatedEvent(post.id, author.id));
        return post;
    }

    public List<Post> getUserPost(Long authorId) {
        return postRepo.findByAuthor(authorId);
    }

    public List<Post> getLatestFeed() {
        return postRepo.findLastest();
    }

    public Post getById(Long postId) {
        return postRepo.findByIdOptional(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    public List<Post> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return postRepo.search(query);
    }

    @Transactional
    public void deletePost(Long postId, Long requestedId) {
        var post = getById(postId);
        if (!post.author.id.equals(requestedId)) {
            throw new IllegalArgumentException("Not your post");
        }
        post.status = PostStatus.DELETED;
    }

    @Transactional
    public void view(Long postId) {
        postRepo.incrementViews(postId);
    }

}
