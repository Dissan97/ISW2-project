package it.torvergata.ahmed.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Objects;

@Getter
public final class Commit {
    private final RevCommit revCommit;
    @Setter
    private Ticket ticket;
    private final Release release;

    public Commit(RevCommit revCommit, Release release) {
        this.revCommit = revCommit;
        this.release = release;
        ticket = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(revCommit, commit.revCommit);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(revCommit);
    }
}

