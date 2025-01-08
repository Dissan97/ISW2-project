package it.torvergata.dissanuddinahmed.model;

import org.eclipse.jgit.revwalk.RevCommit;

public final class Commit {
    private final RevCommit revCommit;
    private Ticket ticket;
    private final Release release;

    public Commit(RevCommit revCommit, Release release) {
        this.revCommit = revCommit;
        this.release = release;
        ticket = null;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Release getRelease() {
        return release;
    }

}

