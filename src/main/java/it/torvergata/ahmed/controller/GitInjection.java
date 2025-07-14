package it.torvergata.ahmed.controller;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import it.torvergata.ahmed.Main;
import it.torvergata.ahmed.logging.SeLogger;
import it.torvergata.ahmed.model.*;
import it.torvergata.ahmed.utilities.CodeSmellParser;
import it.torvergata.ahmed.utilities.JavaParserUtil;
import it.torvergata.ahmed.utilities.Sink;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Class controller responsible to link Jira-Ticket with GitHub commits and fill all the metadata
 */
public class GitInjection {

    private static final String TEMP = ".temp" + File.separator;
    private static final String GIT = File.separator + ".git";
    public static final String PMD_ANALYSIS = "pmdAnalysis";
    public static final String RELEASE = "release";
    public static final String TEST = "Test";
    public static final String JAVA_EXTENTION = ".java";
    public static final String SYS_CUT_PERCENTAGE = "SYS_CUT_PERCENTAGE";
    private final String repoPath;
    private final String lastBranch;
    @Getter
    @Setter
    private List<Ticket> tickets;
    @Getter
    private final List<Release> releases;
    protected final Git localGithub;
    @Getter
    private final Repository repository;
    @Getter
    private List<Commit> commits;
    private ArrayList<Commit> commitsWithIssues;
    private static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    private ArrayList<JavaClass> javaClasses;
    @Getter
    private final Map<RevCommit, List<String>> modifiedClassesForCommit;
    @Getter
    private Map<Release, List<JavaClass>> javaClassPerRelease = null;
    @Getter
    private final String project;
    private final double limitPercentage;
    private final Logger logger = SeLogger.getInstance().getLogger();
    private final String logName;

    /**
     * Constructor of GiraInjection
     *
     * @param targetName  the project target
     * @param targetUrl   the GitHub repository URL
     * @param releaseList all release retrieved in Jira
     */
    public GitInjection(@NotNull String targetName, String targetUrl, List<Release> releaseList)
            throws GitAPIException, IOException {
        this.project = targetName;
        this.repoPath = TEMP + targetName.toLowerCase(Locale.getDefault());
        File directory = new File(repoPath);
        if (!directory.exists()) {
            localGithub = Git.cloneRepository().setURI(targetUrl).setDirectory(directory).call();
            repository = localGithub.getRepository();
        } else {
            repository = new FileRepository(repoPath + GIT);
            localGithub = new Git(repository);
        }
        this.releases = releaseList;
        this.tickets = null;
        this.modifiedClassesForCommit = new HashMap<>();
        this.logName = this.getClass().getSimpleName() + "#" + targetName;
        this.lastBranch = repository.getBranch();
        limitPercentage = Double.parseDouble(System.getProperty(SYS_CUT_PERCENTAGE));
        infoLog("setup percentage for releases: " + limitPercentage);
    }



    private void infoLog(String msg) {
        String info = logName + "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]: " + msg;
        logger.info(info);
    }

    private void errorLog(String msg) {
        String info = logName + "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]: " + msg;
        logger.severe(info);
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
        revCommits.sort(Comparator.comparing(revCommit -> Date.from(
                revCommit.getCommitterIdent().getWhenAsInstant())));
        this.commits = new ArrayList<>();
        for (RevCommit revCommit : revCommits) {
            SimpleDateFormat formatter = new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT);
            LocalDate commitDate = LocalDate.parse(formatter.format(Date.from(revCommit.getCommitterIdent()
                    .getWhenAsInstant())));
            LocalDate lowerBoundDate = LocalDate.parse(formatter.format(new Date(0)));

            for (Release release : this.releases) {
                LocalDate dateOfRelease = release.getReleaseDate();
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
        this.commits.sort(Comparator.comparing(o -> Date.from(o.getRevCommit().getCommitterIdent()
                .getWhenAsInstant())));
    }

    /**
     * Matching JIRA-TICKET ID within the Commit Comment
     */
    public void preprocessCommitsWithIssue() {
        this.commitsWithIssues = new ArrayList<>();

        for (Commit commit : this.commits) {

            for (Ticket ticket : this.tickets) {
                String fullMessage = commit.getRevCommit().getFullMessage();
                String ticketKey = ticket.getTicketKey();
                if (Pattern.compile(ticketKey + "\\b").matcher(fullMessage).find()) {
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

    /**
     * Returning the ClassName and Body
     *
     * @param revCommit the commit in GitHub repo
     * @return The class name touched in that commit and class Body
     */
    public static @NotNull Map<String, String> getAllClassesNameAndContent(@NotNull RevCommit revCommit,
                                                                           Repository repository) throws IOException {
        Map<String, String> allClasses = new HashMap<>();
        RevTree tree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            if (treeWalk.getPathString().contains(JAVA_EXTENTION) && !treeWalk.getPathString().contains("/test/")) {
                allClasses.put(treeWalk.getPathString(), new String(repository.open(treeWalk.getObjectId(0))
                        .getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();
        return allClasses;
    }

    /**
     * Preparing the java Classes linking commit and the Release
     *
     * @see JavaClass
     */
    public void preprocessJavaClasses() throws IOException {
        this.javaClasses = new ArrayList<>();
        List<Commit> latestCommits = new ArrayList<>();

        for (int i = 0; i < this.releases.size(); i++) {
            List<Commit> tempCommits = new ArrayList<>(this.commits);

            int finalI = i;
            tempCommits.removeIf(commit -> (commit.getRelease().getId() != finalI));
            if (tempCommits.isEmpty()) {
                continue;
            }

            latestCommits.add(tempCommits.getLast());
        }

        latestCommits.sort(Comparator.comparing(commit -> Date.from(
                commit.getRevCommit().getCommitterIdent().getWhenAsInstant())));

        for (Commit commit : latestCommits) {
            Map<String, String> nameAndClassContent = getAllClassesNameAndContent(commit.getRevCommit(),
                    this.repository);
            nameAndClassContent.forEach(
                    (name, content) -> {
                        if (!name.contains(TEST)) {
                            javaClasses.add(new JavaClass(name, content,
                                    commit.getRelease(), true));
                        }
                    }
            );
            this.javaClasses.removeIf(javaClass -> !javaClass.isHasMap());

        }

        this.fillClassesInfo();
        infoLog("fillClassInfo");
        this.checkUpdateInClassCommitted();
        infoLog("checkUpdateCommitted");
        this.javaClasses.sort(Comparator.comparing(JavaClass::getName));
        this.parseJavaClassPerRelease();
        infoLog("parsingClassPerRelease: release-size=" + this.javaClassPerRelease.size());
        logBugPercentage();
        // now cutting the 66% of release to do the analysis
        this.cutJavaClasses();
        long start = System.currentTimeMillis();
        infoLog("start check-Age");
        this.checkMethodAge();
        long end = System.currentTimeMillis();
        infoLog("done check Age took=" + ((end - start) / 1e3)  + "s");
        infoLog("start check usage");
        start = System.currentTimeMillis();
        this.checkMethodUsage();
        end = System.currentTimeMillis();
        infoLog("done check Usage took=" + ((end - start) / 1e3)  + "s");
        infoLog("start updateMethodPerClassCommits");
        start = System.currentTimeMillis();
        this.updateMethodPerClassCommits();
        end = System.currentTimeMillis();
        infoLog("done updateMethodPerClassCommits took=" + ((end - start) / 1e3)  + "s");
        infoLog("start check code smells");
        start = System.currentTimeMillis();
        this.setupCodeSmellPMD();
        end = System.currentTimeMillis();
        CodeSmellParser.extractCodeSmell(this.javaClassPerRelease, this.project);
        infoLog("done check code smells took=" + ((end - start) / 1e3)  + "s");
        this.foundMostCodeSmells();


    }

    private void foundMostCodeSmells() {
        AtomicInteger num = new AtomicInteger(0);
        AtomicInteger globalMax = new AtomicInteger(-1);
        Release last = this.javaClassPerRelease.keySet().stream()
                .sorted(Comparator.comparing(Release::getId)).toList().getLast();
        this.javaClassPerRelease.get(last).forEach(
                jc -> {
                    int max = jc.getMethodsMetrics().values().stream().filter(value -> (value.getStatementCount() != 1
                        && value.isBug())
                    )
                            .mapToInt(
                            MethodMetrics::getNumberOfCodeSmells
                    ).max().orElse(0);
                    if (max > globalMax.get()) {
                        globalMax.set(max);
                    }
                }
        );
        this.javaClassPerRelease.get(last).forEach(
                jc -> jc.getMethodsMetrics().entrySet().stream().filter(entry ->
                        (entry.getValue().getStatementCount() != 1) && entry.getValue().isBug())
                        .filter(
                        entry -> entry.getValue().getNumberOfCodeSmells() == globalMax.get()
                ).forEach(
                        entry -> {

                            try {
                                Sink.storeMaxCodeSmells(last.getId() + "smell-"+globalMax.get()+"-"
                                        + this.project + (num.incrementAndGet()), jc, entry);
                            } catch (IOException e) {
                                errorLog(e.getMessage());
                            }

                            extractPreviousMethod(jc, entry, last, globalMax, num);

                        }
                ));

    }

    private void extractPreviousMethod(JavaClass jc, Map.Entry<String, MethodMetrics> entry, Release last, AtomicInteger globalMax, AtomicInteger num) {
        // Trova release precedente
        Optional<Release> previousReleaseOpt = this.javaClassPerRelease.keySet().stream()
                .filter(r -> r.getId() == last.getId() - 1)
                .findFirst();

        if (previousReleaseOpt.isPresent()) {
            Release previousRelease = previousReleaseOpt.get();

            // Cerca la JavaClass con lo stesso nome
            Optional<JavaClass> matchingPrevClassOpt = this.javaClassPerRelease.get(previousRelease).stream()
                    .filter(prevClass -> prevClass.getName().equals(jc.getName()))
                    .findFirst();

            if (matchingPrevClassOpt.isPresent()) {
                JavaClass prevClass = matchingPrevClassOpt.get();

                // Cerca il metodo corrispondente
                if (prevClass.getMethodsMetrics().containsKey(entry.getKey())) {
                    Map.Entry<String, MethodMetrics> prevEntry =
                            Map.entry(entry.getKey(),
                                    prevClass.getMethodsMetrics().get(entry.getKey()));

                    // Store info for previous release
                    try {
                        Sink.storeMaxCodeSmells(
                                previousRelease.getId() + "-prev-smell-" + globalMax.get() +
                                        "-" + this.project + num.get(),
                                prevClass,
                                prevEntry
                        );
                    } catch (IOException e) {
                        errorLog(e.getMessage());
                    }
                }
            }
        }
    }


    private void cutJavaClasses() {
        int limit = (int) (this.javaClassPerRelease.size() * limitPercentage);
        infoLog("LIMIT: " + limit + " total releases size=" + this.javaClassPerRelease.size() +
                " Java classes size=" + this.javaClasses.size());
        javaClassPerRelease.entrySet().removeIf(
                entry -> entry.getKey().getId() > limit
        );
        javaClasses.removeIf(
                javaClass -> javaClass.getRelease().getId() > limit
        );
        infoLog("after cut releases releases size=" + this.javaClassPerRelease.size() +
                "java cut-classes size=" + this.javaClasses.size());
        logBugPercentage();


    }

    private void logBugPercentage() {
        AtomicLong bugCounter = new AtomicLong(0);
        AtomicLong totalCounter = new AtomicLong(0);

        this.javaClassPerRelease.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                .forEach(entry -> {
                    Release theRelease = entry.getKey();
                    List<JavaClass> javaClassesForRelease = entry.getValue();

                    String commitName = theRelease.getCommitList().stream().sorted(Comparator
                            .comparing(commit -> Date.from(
                            commit.getRevCommit().getCommitterIdent().getWhenAsInstant())))
                            .toList().getLast().getRevCommit().getName();
                    StringBuilder msg = new StringBuilder("{\"Release\": ")
                            .append(theRelease.getId())
                            .append("\"Commit\": ")
                            .append(commitName)
                            .append(", \"Bugs\": ");

                    javaClassesForRelease.forEach(javaClass -> {
                        bugCounter.addAndGet(javaClass.getMethodsMetrics().entrySet().stream()
                                .filter(e -> e.getValue().isBug()).count());
                        totalCounter.addAndGet(javaClass.getMethods().size());
                    });

                    msg.append(bugCounter.get());
                    msg.append(", \"Total\": ").append(totalCounter.get());
                    msg.append(", \"Percentage\": \"")
                            .append(String.format("%.2f%%",
                                    100.0 * (bugCounter.get() / Math.max(1.0, totalCounter.get()))))
                            .append("\"}");
                    bugCounter.set(0);
                    totalCounter.set(0);
                    infoLog(msg.toString());

                });
    }

    /**
     * Check method age relative current release
     */
    private void checkMethodAge() {
        Map<String, Integer> methodFirstAppearance = new HashMap<>();
        javaClassPerRelease.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                .forEach(entry -> {
                    Release release = entry.getKey();

                    for (JavaClass javaClass : entry.getValue()) {
                        for (String methodSig : javaClass.getMethodsMetrics().keySet()) {

                            methodFirstAppearance.putIfAbsent(methodSig, release.getId());
                        }
                    }
                });

        javaClassPerRelease.forEach((release, classList) -> {
            for (JavaClass jc : classList) {
                for (Map.Entry<String, MethodMetrics> entry : jc.getMethodsMetrics().entrySet()) {
                    String methodSig = entry.getKey();
                    MethodMetrics metrics = entry.getValue();

                    Integer firstAppearance = methodFirstAppearance.get(methodSig);
                    if (firstAppearance != null) {
                        int age = release.getId() - firstAppearance;
                        metrics.setAge(age);
                    }
                }
            }
        });

    }

    /**
     * Fain in Fan out calculation Method
     */
    private void checkMethodUsage() {
        javaClassPerRelease.forEach((release, classes) -> {

            Map<String, Set<String>> methodCalls = new HashMap<>();

            classes.forEach(jc -> {
                String className = jc.getClassName();
                CompilationUnit cu = StaticJavaParser.parse(jc.getClassBody());

                cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
                    String callerSig = className + "." + JavaParserUtil.getSignature(methodDecl);
                    Set<String> callee = new HashSet<>();

                    methodDecl.findAll(MethodCallExpr.class)
                            .stream()
                            .map(MethodCallExpr::getNameAsString)
                            .forEach(callee::add);

                    methodCalls.put(callerSig, callee);
                });
            });

            classes.forEach(jc -> {
                String className = jc.getClassName();
                jc.getMethodsMetrics().forEach((methodSig, mm) -> {
                    String fullMethodSig = className + "." + methodSig;
                    int fanOut = methodCalls.getOrDefault(fullMethodSig, Collections.emptySet()).size();
                    mm.setFanOut(fanOut);
                });
            });

            classes.forEach(callerJc -> {
                CompilationUnit cu = StaticJavaParser.parse(callerJc.getClassBody());
                cu.findAll(MethodDeclaration.class).forEach(methodDecl ->
                        methodDecl.getBody().ifPresent(block ->
                                block.findAll(MethodCallExpr.class).forEach(callStmt -> {
                                    String calleeName = callStmt.getNameAsString();
                                    int argCount = callStmt.getArguments().size();

                                    classes.forEach(possibleCallee ->
                                            possibleCallee.getMethodsMetrics().values().stream()
                                                    .filter(mm -> mm.getSimpleName().equals(calleeName)
                                                            && mm.getParameterCount() == argCount)
                                                    .forEach(mm -> mm.setFanIn(mm.getFanIn() + 1)));
                                })
                        )
                );
            });

        });
    }

    /**
     * Use PMD to calculate codeSmells
     */
    private void setupCodeSmellPMD() {
        String outputDirPath = PMD_ANALYSIS + File.separator + this.project;
        File outputDir = new File(outputDirPath);

        // Skip PMD if the output directory exists and contains enough files

        if (outputDir.exists() && outputDir.isDirectory()) {
            File[] existingFiles = outputDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (existingFiles != null && existingFiles.length >= this.javaClassPerRelease.size()) {
                return;
            }
        }
        this.javaClassPerRelease.keySet()
                .stream().sorted(Comparator.comparing(Release::getId)).forEach(
                        release -> {

                            if (release.getId() <= 1) {
                                infoLog(RELEASE + " " + 0 + " starting");
                                doPmd(0, release.getCommitList().getFirst().getRevCommit());
                            }

                            infoLog(RELEASE + " " + release.getId() + " starting");


                            doPmd(release.getId(), release.getCommitList().getLast().getRevCommit());
                        }
                );
    }

    private void doPmd(int releaseId, @NotNull RevCommit commit) {
        String commitId = commit.getName();
        final String reportPath = PMD_ANALYSIS + File.separator + this.project + File.separator
                + releaseId + ".csv";
        if ((new File(reportPath).exists())) {
            return;
        }
        try {
            String msg = "do pmd for " + this.project + " previous " + RELEASE + " " + releaseId +
                    " with commit " + commitId + " for next " + RELEASE + " " + (releaseId + 1);
            SeLogger.getInstance().getLogger().info(msg);

            this.localGithub.checkout().setForced(true).setName(commitId).call();

        } catch (GitAPIException e) {
            SeLogger.getInstance().getLogger().warning("Initial checkout failed for " + commitId + ": " + e.getMessage());
            try {
                // Ripristina lo stato del repo
                cleanGitState();  // definita sotto
                this.localGithub.reset().setMode(ResetCommand.ResetType.HARD).call();
                this.localGithub.clean().setCleanDirectories(true).call();

                // Retry checkout
                this.localGithub.checkout().setForced(true).setName(commitId).call();

            } catch (GitAPIException | IOException ex) {
                String msg = commitId + " still has problems after reset: " + ex.getMessage();
                SeLogger.getInstance().getLogger().severe(msg);
            }
        } finally {
            restoreRepositoryState();
        }
        try {
            Process process = getProcess(releaseId);
            process.waitFor(5L, TimeUnit.MINUTES);
            String msg = "done pmd " + this.project + " " + RELEASE + " " + releaseId;
            SeLogger.getInstance().getLogger().info(msg);
            process.destroy();
        } catch (IOException ignored) {
            // go next
        } catch (InterruptedException e) {
            String msg = "Some problem occurred here unexpected interruption " + this.project + " " + RELEASE + " " + releaseId;
            SeLogger.getInstance().getLogger().info(msg);
            Thread.currentThread().interrupt();
        }
    }


    private void cleanGitState() throws IOException {
        String gitDir = repoPath + File.separator + ".git" + File.separator;
        String[] mergeFiles = {"MERGE_HEAD", "MERGE_MSG", "MERGE_MODE", "index"};
        for (String file : mergeFiles) {
            File f = new File(gitDir + file);
            if (f.exists()) {
                Files.delete(f.toPath());
            }
        }
    }


    @Contract("_ -> new")
    private @NotNull Process getProcess(int id) throws IOException {
        final String pmd = System.getenv(Main.SYS_PMD_HOME) + File.separator + "bin" + File.separator + "pmd";
        final String reportPath = PMD_ANALYSIS + File.separator + this.project + File.separator + id + ".csv";
        return new ProcessBuilder(
                pmd,
                "check",
                "-d", this.repoPath,
                "-R", Objects.requireNonNull(GitInjection.class
                .getClassLoader()
                .getResource("pmd/custom.xml")).getPath(),
                "-f", "csv",
                "--no-cache",
                "-r", reportPath
        ).redirectErrorStream(true).start();
    }

    private void updateMethodPerClassCommits() {
        AtomicLong counter = new AtomicLong(0);
        try {
            this.javaClassPerRelease.forEach(
                    (release, jc) -> jc.stream().filter(
                            javaClass -> {
                                if (javaClass.getClassCommits() != null) {
                                    return javaClass.getClassCommits().size() > 1;
                                }
                                return false;
                            }
                    ).forEach(
                            javaClass -> javaClass.getClassCommits().forEach(
                                    commit -> {
                                        if (!commit.equals(javaClass.getClassCommits().getLast())) {
                                            moreClassInfo(javaClass, commit.getRevCommit());
                                            counter.incrementAndGet();
                                        }
                                    }
                            )
                    )
            );
        } catch (NullPointerException ignored) {
            // skip the class which has no commit
        } finally {
            restoreRepositoryState();
        }
        infoLog("updateMethodPerClassCommits commits: " + counter.get());
    }

    private void moreClassInfo(@NotNull JavaClass javaClass, @NotNull RevCommit revCommit) {
        RevTree tree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        try {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathSuffixFilter.create(JAVA_EXTENTION));
            while (treeWalk.next()) {
                if (treeWalk.getPathString().contains(javaClass.getName())) {
                    CompilationUnit cu = StaticJavaParser.parse(new String(repository
                            .open(treeWalk.getObjectId(0))
                            .getBytes(), StandardCharsets.UTF_8));
                    cu.findAll(MethodDeclaration.class).forEach(
                            methodDeclaration -> {
                                String methodName = methodDeclaration.getDeclarationAsString();
                                if (javaClass.getMethods().get(methodName) != null) {
                                    methodDeclaration.getBody().ifPresent(
                                            body -> calcDiff(javaClass, methodDeclaration, methodName, revCommit)
                                    );
                                }
                            }
                    );
                }


            }

        } catch (IOException | ParseProblemException ipe) {
            // ignoring this
            errorLog(ipe.getClass().getSimpleName() + " problem with this javaClass in release: "
                    + javaClass.getRelease().getId());
        } catch (NullPointerException e) {
            // ignoring this step
            errorLog("java class: " + javaClass.getName() + " nullptr: " +
                    e.getMessage());
        }
        treeWalk.close();
    }


    private static void calcDiff(@NotNull JavaClass javaClass, MethodDeclaration methodDeclaration,
                                 String methodName, @NotNull RevCommit revCommit) {
        String newBody = JavaParserUtil.getStringBody(methodDeclaration);
        String oldBody = javaClass.getMethods().get(methodName);
        if (!newBody.equals(oldBody)) {
            int added = 0;
            int removed = 0;
            List<String> oldLines = Arrays.asList(oldBody.split("\n"));
            List<String> newLines = Arrays.asList(newBody.split("\n"));
            for (String line : newLines) {
                if (!oldLines.contains(line)) added++;
            }
            for (String line : oldLines) {
                if (!newLines.contains(line)) removed++;
            }
            javaClass.getMethodsMetrics().get(methodName).incChanges();
            javaClass.getMethodsMetrics().get(methodName)
                    .addChurn(added, removed);
            javaClass.getMethodsMetrics().get(methodName).addAuthor(revCommit.getAuthorIdent().getName());
        }
    }

    private void parseJavaClassPerRelease() {
        this.javaClassPerRelease = new LinkedHashMap<>();
        for (JavaClass javaClass : this.javaClasses) {
            javaClassPerRelease.putIfAbsent(javaClass.getRelease(), new ArrayList<>());
            javaClassPerRelease.get(javaClass.getRelease()).add(javaClass);
        }
    }


    /**
     * Adding all commits for a Release to a Class
     */
    private void checkUpdateInClassCommitted() throws IOException {
        List<JavaClass> tempProjClasses;
        for (Commit commit : this.commits) {
            Release release = commit.getRelease();
            tempProjClasses = new ArrayList<>(this.javaClasses);
            tempProjClasses.removeIf(tempProjClass -> !tempProjClass.getRelease().equals(release));
            List<String> modifiedClassesNames = this.getTouchedClassesNames(commit.getRevCommit());
            for (String modifiedClass : modifiedClassesNames) {
                for (JavaClass javaClass : tempProjClasses) {
                    if ((javaClass.getName().equals(modifiedClass)) &&
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

    /**
     * Checking if a class contains or not a bug
     *
     * @param theTickets jira Tickets
     * @param theClasses all javaClasses
     */
    public void fillClassesInfo(List<Ticket> theTickets, @NotNull List<JavaClass> theClasses) throws IOException {
        for (JavaClass javaClass : theClasses) {
            javaClass.getMetrics().setBug(false);
        }

        for (Ticket ticket : theTickets) {
            List<Commit> commitsContainingTicket = ticket.getCommitList();
            Release injectedVersion = ticket.getInjectedVersion();
            for (Commit commit : commitsContainingTicket) {
                SimpleDateFormat formatter = new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT);
                RevCommit revCommit = commit.getRevCommit();
                LocalDate commitDate = LocalDate.parse(formatter.format(
                        Date.from(revCommit.getCommitterIdent().getWhenAsInstant())));
                if (!commitDate.isAfter(ticket.getResolutionDate())
                        && !commitDate.isBefore(ticket.getCreationDate())) {
                    List<String> modifiedClassesNames = getTouchedClassesNames(revCommit);
                    Release releaseOfCommit = commit.getRelease();
                    modifiedClassesForCommit.putIfAbsent(revCommit, modifiedClassesNames);
                    for (String modifiedClass : modifiedClassesNames) {
                        checkForAnyBug(modifiedClass, injectedVersion, releaseOfCommit);
                    }

                }
            }
        }
    }

    /**
     * Check if a class or method contains a bug
     *
     * @param modifiedClass   all classes modified in that Release
     * @param injectedVersion the injected version of the bug
     * @param fixedVersion    the fixed version of the bug
     */

    private void checkForAnyBug(String modifiedClass, Release injectedVersion, Release fixedVersion) {
        // by marking the class as a buggy, this can be removed

        List<JavaClass> fixedClasses = this.javaClasses.stream().filter(javaClass -> javaClass.getRelease().getId()
                        == fixedVersion.getId()).toList();

        for (JavaClass javaClass : this.javaClasses) {
            if (javaClass.getName().equals(modifiedClass)
                    && javaClass.getRelease().getId() < fixedVersion.getId()
                    && javaClass.getRelease().getId() >= injectedVersion.getId()) {
                javaClass.getMetrics().setBug(true);
                javaClass.getMethods().entrySet().forEach(entry ->
                        fixedClasses.stream().filter(jc -> jc.getName().equals(modifiedClass)).findAny().ifPresent(
                        fixedClass -> {
                            Map<String, String> methodMap = fixedClass.getMethods();
                            checkMethodDiff(javaClass, entry, methodMap);
                        }
                ));

            }
        }

    }

    private static void checkMethodDiff(JavaClass javaClass, Map.Entry<String, String> entry,
                                        Map<String, String> methodMap) {
        if (!methodMap.containsKey(entry.getKey())) {
            javaClass.getMethodsMetrics().get(entry.getKey()).setBug(true);
        }else {
            String fixedBody = (methodMap.get(entry.getKey()));
            String oldBody = (entry.getValue());
            if (!fixedBody.equals(oldBody)) {
                javaClass.getMethodsMetrics().get(entry.getKey()).setBug(true);
            }
        }
    }

    /**
     * Check which class is touched in a commit
     *
     * @param commit the commit id
     * @return all classes touched in a commit
     */

    private @NotNull List<String> getTouchedClassesNames(@NotNull RevCommit commit) throws IOException {
        List<String> touchedClassesNames = new ArrayList<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
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
            for (DiffEntry entry : entries) {
                if (entry.getNewPath().contains(JAVA_EXTENTION) && !entry.getNewPath().contains("/test/")) {
                    touchedClassesNames.add(entry.getNewPath());
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
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

    public void checkLOCInfo(@NotNull JavaClass javaClass) {
        for (Commit commit : javaClass.getClassCommits()) {
            RevCommit revCommit = commit.getRevCommit();
            try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                RevCommit parentComm = revCommit.getParent(0);
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                List<DiffEntry> diffEntries = diffFormatter.scan(parentComm.getTree(), revCommit.getTree());
                for (DiffEntry diffEntry : diffEntries) {
                    if (diffEntry.getNewPath().equals(javaClass.getName())) {
                        javaClass.addLOCAddedByClass(getAddedLines(diffFormatter, diffEntry));
                        javaClass.addLOCRemovedByClass(getDeletedLines(diffFormatter, diffEntry));
                    }
                }
            } catch (ArrayIndexOutOfBoundsException | IOException ignored) {
                //ignoring when no parent is found
            }
        }
    }

    private int getAddedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int addedLines = 0;
        for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;
    }

    private int getDeletedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int deletedLines = 0;
        for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
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
                ids.add(release.getReleaseName());
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
        for (Commit commit : this.commits) {
            Map<String, String> inner = new LinkedHashMap<>();
            RevCommit revCommit = commit.getRevCommit();
            Ticket ticket = commit.getTicket();
            Release release = commit.getRelease();
            if (ticket != null) {
                inner.put("ticketKey", commit.getTicket().getTicketKey());
            }
            inner.put(RELEASE, release.getReleaseName());
            inner.put("creationDate",
                    String.valueOf(LocalDate.parse((new SimpleDateFormat(GitInjection.LOCAL_DATE_FORMAT)
                            .format(Date.from(revCommit.getCommitterIdent().getWhenAsInstant()))
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

    private void restoreRepositoryState() {
        if (lastBranch == null) {
            errorLog("No branch to restore to");
            return;
        }
        try {
            // reset e clean
            localGithub.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .call();
            localGithub.clean()
                    .setCleanDirectories(true)
                    .call();

            // torna al branch salvato
            localGithub.checkout()
                    .setName(lastBranch)
                    .call();

            infoLog("Repository restored to branch " + lastBranch + " at HEAD");
        } catch (GitAPIException e) {
            errorLog("reset the repo: " + e.getMessage());
        }
    }


}
