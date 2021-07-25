package view.builder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import view.builder.utils.ObjectMapperUtils;

import java.util.List;

public class Post implements IdInterface<Long>, UserIdInterface<Integer> {
    private final long id;
    private final int userId;
    private final List<Long> commentIds;
    @JsonIgnore
    private List<Comment> comments;

    public Post(long id, int userId, List<Long> commentIds) {
        this.id = id;
        this.userId = userId;
        this.commentIds = commentIds;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    public List<Comment> comments() {
        return comments;
    }

    public List<Long> getCommentIds() {
        return commentIds;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return ObjectMapperUtils.toJSON(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Post)) {
            return false;
        }
        Post other = (Post) obj;
        return id == other.id;
    }
}
