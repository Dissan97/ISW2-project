package it.torvergata.dissanuddinahmed.controller;

import it.torvergata.dissanuddinahmed.model.Commit;
import it.torvergata.dissanuddinahmed.model.JavaClass;
import it.torvergata.dissanuddinahmed.model.Release;
import it.torvergata.dissanuddinahmed.model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

public class GitInjection {

    private static final String TEMP = ".temp" + File.separator;
    private static final String GIT = File.separator+ ".git";

    private List<Ticket> tickets;
    private final List<Release> releases;
    protected final Git localGithub;
    private final Repository repository;
    private List<Commit> commits;
    private ArrayList<Commit> commitsWithIssues;
    private static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";



    private ArrayList<JavaClass> javaClasses;

    public GitInjection(@NotNull String targetName, String targetUrl, List<Release> releaseList)
            throws GitAPIException, IOException {
        String path = TEMP + targetName.toLowerCase(Locale.getDefault());
        File directory = new File(path);
        if (!directory.exists()) {
            localGithub = Git.cloneRepository().setURI(targetUrl).setDirectory(directory).call();
            repository = localGithub.getRepository();
        }else {
            repository = new FileRepository(path + GIT);
            localGithub = new Git(repository);
        }
        this.releases = releaseList;
        this.tickets = null;
    }

    /**
     * used to inject commits in the revCommitList
     */
    public void injectCommits() throws GitAPIException, IOException {
        List<RevCommit> revCommits = new ArrayList<>();
        List<Ref> allBranch = localGithub.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref branch : allBranch) {
            Iterable<RevCommit> branchCommits = localGithub.log().add(this.repository.resolve(branch.getName())).call();
            for (RevCommit branchCommit : branchCommits) {
                if (!revCommits.contains(branchCommit)) {
                    revCommits.add(branchCommit);
                }
            }
        }
        revCommits.sort(Comparator.comparing(revCommit -> revCommit.getCommitterIdent().getWhen()));
        this.commits = new ArrayList<>();
        for (RevCommit revCommit : revCommits) {
            SimpleDateFormat formatter = new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT);
            LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));
            LocalDate lowerBoundDate = LocalDate.parse(formatter.format(new Date(0)));

            for(Release release: this.releases) {
                LocalDate dateOfRelease = release.releaseDate();
                if (commitDate.isAfter(lowerBoundDate) && !commitDate.isAfter(dateOfRelease)) {
                    Commit newCommit = new Commit(revCommit, release);
                    this.commits.add(newCommit);
                    release.addCommit(newCommit);
                }
                lowerBoundDate = dateOfRelease;
            }
        }
        this.releases.removeIf(release -> release.getCommitList().isEmpty());
        int i = 0;
        for (Release release : this.releases) {
            release.setId(++i);
        }
        this.commits.sort(Comparator.comparing(o -> o.getRevCommit().getCommitterIdent().getWhen()));
    }

    public List<Release> getReleases() {
        return releases;
    }

    public void setTickets(List<Ticket> fixedTickets) {
        this.tickets = fixedTickets;
    }

    public void preprocessCommitsWithIssue() {
        this.commitsWithIssues = new ArrayList<>();

        for (Commit commit : this.commits) {

            for (Ticket ticket : this.tickets) {
                String fullMessage = commit.getRevCommit().getFullMessage();
                String ticketKey = ticket.getTicketKey();
                if (Pattern.compile(ticketKey+"\\b").matcher(fullMessage).find()) {
                    this.commitsWithIssues.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }
        this.tickets.removeIf(ticket -> ticket.getCommitList().isEmpty());
    }

    public void closeRepo() {
        this.localGithub.getRepository().close();
    }

    private @NotNull Map<String, String> getAllClassesNameAndContent(@NotNull RevCommit revCommit) throws IOException {
        Map<String, String> allClasses = new HashMap<>();
        RevTree tree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while(treeWalk.next()) {
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                allClasses.put(treeWalk.getPathString(), new String(repository.open(treeWalk.getObjectId(0))
                        .getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();
        return allClasses;
    }

    public void preprocessJavaClasses() throws IOException {
        this.javaClasses = new ArrayList<>();
        List<Commit> latestCommits = new ArrayList<>();
        for (int i = 0; i < this.releases.size(); i++) {
            List<Commit> tempCommits = new ArrayList<>(this.commits);
            int finalI = i;
            tempCommits.removeIf(commit -> (commit.getRelease().id() != finalI));
            if (tempCommits.isEmpty()) {
                continue;
            }
            latestCommits.add(tempCommits.get(tempCommits.size() - 1));
        }

        latestCommits.sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitterIdent().getWhen()));
        for (Commit commit : latestCommits) {
            Map<String, String> nameAndClassContent = getAllClassesNameAndContent(commit.getRevCommit());
            for(Map.Entry<String, String> nameAndContentOfClass : nameAndClassContent.entrySet()){
                javaClasses.add(new JavaClass(nameAndContentOfClass.getKey(), nameAndContentOfClass.getValue(),
                        commit.getRelease()));
            }
        }

        this.fillClassesInfo();
        this.checkUpdateInClassCommitted();
        this.javaClasses.sort(Comparator.comparing(JavaClass::getName));
    }

    private void checkUpdateInClassCommitted() throws IOException {
        List<JavaClass> tempProjClasses;
        for(Commit commit: this.commits) {
            Release release = commit.getRelease();
            tempProjClasses = new ArrayList<>(this.javaClasses);
            tempProjClasses.removeIf(tempProjClass -> !tempProjClass.getRelease().equals(release));
            List<String> modifiedClassesNames = this.getTouchedClassesNames(commit.getRevCommit());
            for(String modifiedClass: modifiedClassesNames){
                for(JavaClass javaClass: tempProjClasses){
                    if((javaClass.getName().equals(modifiedClass)) &&
                            (!javaClass.getClassCommits().contains(commit))) {
                        javaClass.addCommitToClass(commit);
                    }
                }
            }
        }
    }
    private void fillClassesInfo() throws IOException {
        this.fillClassesInfo(this.tickets, this.javaClasses);
    }
    public void fillClassesInfo(List <Ticket> theTickets, List<JavaClass> theClasses) throws IOException {
        for(JavaClass javaClass: theClasses) {
            javaClass.getMetrics().setBuggyness(false);
        }
        for(Ticket ticket: theTickets) {
            List<Commit> commitsContainingTicket = ticket.getCommitList();
            Release injectedVersion = ticket.getInjectedVersion();
            for (Commit commit : commitsContainingTicket) {
                SimpleDateFormat formatter = new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT);
                RevCommit revCommit = commit.getRevCommit();
                LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));
                if (!commitDate.isAfter(ticket.getResolutionDate())
                        && !commitDate.isBefore(ticket.getCreationDate())) {
                    List<String> modifiedClassesNames = getTouchedClassesNames(revCommit);
                    Release releaseOfCommit = commit.getRelease();
                    for (String modifiedClass : modifiedClassesNames) {
                        checkForBuggyClass(modifiedClass, injectedVersion, releaseOfCommit);
                    }
                }
            }
        }
    }

    private void checkForBuggyClass(String modifiedClass, Release injectedVersion, Release fixedVersion) {
        for(JavaClass javaClass: this.javaClasses) {
            if(
                    (javaClass.getName().equals(modifiedClass)) &&
                    (javaClass.getRelease().id() < fixedVersion.id()) &&
                    (javaClass.getRelease().id() >= injectedVersion.id())
            ){
                javaClass.getMetrics().setBuggyness(true);
            }
        }
    }

    private List<String> getTouchedClassesNames(RevCommit commit) throws IOException {
        List<String> touchedClassesNames = new ArrayList<>();
        try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = commit.getTree();
            newTreeIter.reset(reader, newTree);
            RevCommit commitParent = commit.getParent(0);
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = commitParent.getTree();
            oldTreeIter.reset(reader, oldTree);
            diffFormatter.setRepository(repository);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
            for(DiffEntry entry : entries) {
                if(entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test/")) {
                    touchedClassesNames.add(entry.getNewPath());
                }
            }
        } catch(ArrayIndexOutOfBoundsException ignored) {
            //ignoring when no parent is found
        }
        return touchedClassesNames;
    }

    public List<Commit> getCommitsWithIssues() {
        return commitsWithIssues;
    }

    public List<JavaClass> getJavaClasses() {
        return this.javaClasses;
    }

    public void checkLOCInfo(JavaClass javaClass) {
        for(Commit commit : javaClass.getClassCommits()) {
            RevCommit revCommit = commit.getRevCommit();
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                RevCommit parentComm = revCommit.getParent(0);
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                List<DiffEntry> diffEntries = diffFormatter.scan(parentComm.getTree(), revCommit.getTree());
                for(DiffEntry diffEntry : diffEntries) {
                    if(diffEntry.getNewPath().equals(javaClass.getName())) {
                        javaClass.addLOCAddedByClass(getAddedLines(diffFormatter, diffEntry));
                        javaClass.addLOCRemovedByClass(getDeletedLines(diffFormatter, diffEntry));
                    }
                }
            } catch(ArrayIndexOutOfBoundsException | IOException ignored) {
                //ignoring when no parent is found
            }
        }
    }

    private int getAddedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int addedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;
    }

    private int getDeletedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int deletedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            deletedLines += edit.getEndA() - edit.getBeginA();
        }
        return deletedLines;
    }

    public Map<String, String> getMapTickets() {
        Map<String, String> mapTickets = new HashMap<>();
        this.tickets.sort(Comparator.comparing(Ticket::getCreationDate));
        for (Ticket ticket : this.tickets) {
            List<String> ids = new ArrayList<>();
            for (Release release : ticket.getAffectedVersions()) {
                ids.add(release.releaseName());
            }
            Map<String, String> inner = new LinkedHashMap<>();
            inner.put("injectedVersion", ticket.getInjectedVersion().toString());
            inner.put("openingVersion", ticket.getOpeningVersion().toString());
            inner.put("fixedVersion", ticket.getFixedVersion().toString());
            inner.put("affectedVersions", ids.toString());
            inner.put("commits", String.valueOf(ticket.getCommitList().size()));
            inner.put("creationDate", ticket.getCreationDate().toString());
            inner.put("resolutionDate", ticket.getResolutionDate().toString());
            mapTickets.put(ticket.getTicketKey(), inner.toString());
        }

        return mapTickets;
    }

    public Map<String, String> getMapCommits() {
        Map<String, String> mapCommits = new HashMap<>();
        for (Commit commit: this.commits) {
            Map<String, String> inner = new LinkedHashMap<>();
            RevCommit revCommit = commit.getRevCommit();
            Ticket ticket = commit.getTicket();
            Release release = commit.getRelease();
            if (ticket != null) {
                inner.put("ticketKey", commit.getTicket().getTicketKey());
            }
            inner.put("release", release.releaseName());
            inner.put("creationDate",
                    String.valueOf(LocalDate.parse((new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT)
                            .format(revCommit.getCommitterIdent().getWhen())
                    ))));
            mapCommits.put(revCommit.getName(), inner.toString());
        }
        return mapCommits;
    }

    public Map<String, String> getMapSummary() {
        Map<String, String> summaryMap = new HashMap<>();
        summaryMap.put("Releases", String.valueOf(this.releases.size()));
        summaryMap.put("Tickets", String.valueOf(this.tickets.size()));
        summaryMap.put("Commits", String.valueOf(this.commits.size()));
        summaryMap.put("Commits with bugs", String.valueOf(this.commitsWithIssues.size()));
        return summaryMap;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }
}
