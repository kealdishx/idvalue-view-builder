package view.builder.model;

import view.builder.utils.ObjectMapperUtils;

import java.util.List;

public class Comment implements IdInterface<Long>, UserIdInterface<Integer> {
    private final long id;
    private final int userId;
    private final List<Integer> atUserIds;

    public Comment(long id, int userId, List<Integer> atUserIds) {
        this.id = id;
        this.userId = userId;
        this.atUserIds = atUserIds;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public Long getId() {
        return id;
    }

    public List<Integer> getAtUserIds() {
        return atUserIds;
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
        if (!(obj instanceof Comment)) {
            return false;
        }
        Comment other = (Comment) obj;
        return id == other.id;
    }
}
