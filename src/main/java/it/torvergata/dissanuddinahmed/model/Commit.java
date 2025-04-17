package it.torvergata.dissanuddinahmed.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.revwalk.RevCommit;

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

}

