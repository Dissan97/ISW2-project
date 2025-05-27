package it.uniroma2.ahmed.utilities;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.util.List;

public class JgitUtils {

    private JgitUtils() {
        // this is a StaticUtils
    }

    public static boolean commitModifiesFiles(Repository repo, RevCommit commit, String filePath) throws IOException {
        if (commit.getParentCount() == 0) {
            return false;
        }
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, commit.getParent(0).getTree());

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, commit.getTree());

            try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                diffFormatter.setRepository(repo);
                List<DiffEntry> diffs = diffFormatter.scan(oldTreeIter, newTreeIter);
                for (DiffEntry diff : diffs) {
                    if (diff.getNewPath().equals(filePath) || diff.getOldPath().equals(filePath)) {
                        return true;
                    }
                }
            }
        }
        return false;


    }
}
