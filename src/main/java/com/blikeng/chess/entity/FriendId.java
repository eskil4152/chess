package com.blikeng.chess.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.blikeng.chess.exception.types.FriendYourselfException;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class FriendId implements Serializable {
    protected FriendId() {}

    private UUID userA;
    private UUID userB;

    public FriendId(UUID userA, UUID userB) {
        this.userA = userA;
        this.userB = userB;
    }

    public static FriendId generate(UUID a, UUID b) {
        if (a.equals(b)) {
            throw new FriendYourselfException();
        }

        if (a.compareTo(b) < 0) {
            return new FriendId(a, b);
        }

        return new FriendId(b, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendId other)) return false;
        return Objects.equals(userA, other.userA) && Objects.equals(userB, other.userB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userA, userB);
    }
}
