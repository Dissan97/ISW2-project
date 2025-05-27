package it.uniroma2.ahmed.model;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Ticket {
    private final String ticketKey;

    private final LocalDate creationDate;
    private final LocalDate resolutionDate;
    @Setter
    private Release injectedVersion;
    private final Release openingVersion;
    private final Release fixedVersion;
    @Setter
    private List<Release> affectedVersions;
    private final List<Commit> commitList;

    public Ticket(String ticketKey, LocalDate creationDate, LocalDate resolutionDate, Release openingVersion,
                  Release fixedVersion, @NotNull List<Release> affectedVersions) {
        this.ticketKey = ticketKey;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        if(affectedVersions.isEmpty()){
            injectedVersion = null;
        }else{
            injectedVersion = affectedVersions.getFirst();
        }
        this.openingVersion = openingVersion;
        this.fixedVersion = fixedVersion;
        this.affectedVersions = affectedVersions;
        commitList = new ArrayList<>();
    }

    public void addCommit(Commit newCommit) {
        if(!commitList.contains(newCommit)){
            commitList.add(newCommit);
        }
    }
    public boolean isCorrectTicket() {
        return !this.getAffectedVersions().isEmpty();
    }

}

