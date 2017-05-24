package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.io.IOException;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Andrew Leong and Khalil Joseph
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws
        IOException, ClassNotFoundException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else {
            commandParse(args);
        }
    }

    /** Checks to see if a gitlet directory exists. If not, throws an error. */
    static void isInitialized() {
        File getlit = new File("./.gitlet");
        if (!getlit.exists()) {
            System.err.println("Not in an initialized .gitlet directory.");
            System.exit(0);
        }
    }

    /** Checks that there are two operands in the command ARGS. */
    public static void twoParse(String... args) {
        isInitialized();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Calls appropriate command given by ARGS. */
    public static void commandParse(String... args)
        throws IOException {
        switch (args[0]) {
        case "init":
            Repo r = new Repo();
            break;
        case "add":
            add(args);
            break;
        case "commit":
            commit(args);
            break;
        case "rm":
            rm(args);
            break;
        case "status":
            isInitialized();
            if (args.length > 1) {
                System.out.println("Incorrect operands.");
                break;
            }
            status();
            break;
        case "find":
            twoParse(args);
            find(args[1]);
            break;
        case "log":
            logParse(args);
            break;
        case "global-log":
            globalog(args);
            break;
        case "merge":
            mergeParse(args);
            break;
        case "reset":
            twoParse(args);
            reset(args[1]);
            break;
        case "checkout":
            checkoutParse(args);
            break;
        case "rm-branch":
            twoParse(args);
            rembranch(args[1]);
            break;
        case "branch":
            twoParse(args);
            branch(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    /** Parses the merge command given by ARGS. */
    public static void mergeParse(String... args) {
        isInitialized();
        twoParse();
        mergeEdge(args[1]);
    }

    /** Tests failure and edge cases for merge, and if none apply, performs
    the merge. GIVEN is the name of the branch we're merging with.  */
    public static void mergeEdge(String given) {
        Repo r = new Repo("");
        Branch current = r.getCurrent();
        File givenFile = new File("./.gitlet/global/" + given);
        if (!givenFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Branch givenBranch = loadBranch(givenFile);
        String newid = givenBranch.getSHA();
        String oldid = current.getSHA();
        checkForUntracked(oldid, newid);
        File stage = new File("./.gitlet/stage");
        File[] staged = stage.listFiles();
        if (staged.length > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (givenBranch.equals(current)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        LinkedList<String> ancestors = current.getChained();
        LinkedList<String> histories = givenBranch.getChained();
        if (ancestors.contains(newid)) {
            System.out.println("Given branch is an ancestor of the current branch.");
        } else if (histories.contains(oldid)) {
            current.setSHA(newid);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        } else {
            String splitPt = twoRoadsDiverged(ancestors, histories);
            oldid = oldid.substring(0, 6);
            newid = newid.substring(0, 6);
            boolean conflicted = merge(oldid, newid, splitPt, current.getName());
            if (conflicted) {
                System.out.println("Encountered a merge conflict.");
                System.exit(0);
            } else {
                current.commit(
                    "Merged " + current.getName() + " with " 
                    + givenBranch.getName());
            }
        }
        current.store();
        givenBranch.store();
    }

    /** Finds the split point of two branch histories A and B. 
    * "Two roads diverged in a yellow wood/
    * And sorry I could not travel both/
    * And be one traveler, long I stood/
    * And looked down one as far as I could/
    * To where it bent in the undergrowth..."
    */
    public static String twoRoadsDiverged(
        LinkedList<String> A, LinkedList<String> B) {
        Iterator<String> aItr = A.iterator();
        Iterator<String> bItr = B.iterator();
        String splitPt = aItr.next();
        bItr.next();
        while (aItr.hasNext()
            && bItr.hasNext()) {
            String one = aItr.next();
            String two = bItr.next();
            if (one != two) {
                break;
            } else {
                splitPt = one;
            }
        }
        return splitPt.substring(0, 6);
    }

    /** Merges CURRENT with GIVEN; i.e. integrates changes from commit
    GIVEN into commit CURRENT on branch CURRANT that have happened since
    commit SPLIT. Returns true if a conflict occurs. */
    public static boolean merge(
        String current, String given, String split, String currant) {
        boolean conflicting = false;
        File givenfile = new File("./.gitlet/versions/" + given);
        File currentfile = new File("./.gitlet/versions/" + current);
        File splitfile = new File("./.gitlet/versions/" + split);
        List<String> allFiles = Utils.plainFilenamesIn(givenfile);
        allFiles.addAll(0, Utils.plainFilenamesIn(currentfile));
        allFiles.addAll(0, Utils.plainFilenamesIn(splitfile));
        Iterator<String> iter = allFiles.iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            File currentVersion =
                new File("./.gitlet/versions/" + current + "/" + name);
            File splitVersion =
                new File("./.gitlet/versions/" + split + "/" + name);
            File g =
                new File("./.gitlet/versions/" + given + "/" + name);
            String cSHA = toSHA(currentVersion);
            String gSHA = toSHA(g);
            String sSHA = toSHA(splitVersion);
            conflicting = choices(
                currentVersion, splitVersion, g, cSHA, gSHA, sSHA, name,
                current, given, currant);
        }
        return conflicting;
    }

    /** Modifies CURRENTVERSION, SPLITVERSION, and G versions of file NAME
    according to which have been modified, added, or deleted. CSHA, GSHA, SSHA
    are used to compare the contents of each file. Returns TRUE if a
    merge conflict occurs. */
    private static boolean choices(File currentVersion, File splitVersion,
        File g, String cSHA, String gSHA, String sSHA, String name, 
        String current, String given, String currant) {
        if (g.exists()) {
            if (currentVersion.exists()
                && splitVersion.exists()) {
                if (!sSHA.equals(gSHA)) {
                    if (sSHA.equals(cSHA)) {
                        checkoutFile(name, given);
                        File overwritten = new File(name);
                        stage(overwritten);
                    } else if (!cSHA.equals(gSHA)) {
                        conflictManagement(currentVersion, g);
                        return true;
                    } else {
                        return false;
                    }
                }
            } else if (!splitVersion.exists()) {
                if (currentVersion.exists()) {
                    if (!cSHA.equals(gSHA)) {
                        conflictManagement(currentVersion, g);
                        return true;
                    }
                } else {
                    checkoutFile(name, given);
                    File overwrite = new File(name);
                    stage(overwrite);
                }
            } else {
                if (!sSHA.equals(gSHA)) {
                    conflictManagement(null, g);
                    return true;
                }
            }
        } else if (splitVersion.exists()) {
            if (currentVersion.exists()
                && cSHA.equals(sSHA)) {
                rm(name, current, currant);
            } else {
                conflictManagement(currentVersion, null);
                return true;
            }
        } else {
            return false;
        }
        return false;
    }

    /** Writes file to working directory showing conflicting passages.
    Does not stage result. */
    private static void conflictManagement(File current, File given) {
        if (current == null || given == null) {
            return;
        }
    }

    /** Parses the log command given by  ARGS. */
    public static void logParse(String... args) {
        isInitialized();
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Repo man = new Repo("dummy");
        Branch current = man.getCurrent();
        log(current);
    }

    /** Parses ARGS for a checkout command. */
    public static void checkoutParse(String... args) {
        isInitialized();
        if (args.length == 2) {
            checkoutBranch(args[1]);
        } else if (args.length == 3
            && args[1].equals("--")) {
            Repo r = new Repo("");
            String current = r.getCurrent().getSHA();
            checkoutFile(args[2], current);
            r.getCurrent().store();
        } else if (args.length == 4
            && args[2].equals("--")) {
            checkoutFile(args[3], args[1]);
        } else {
            System.out.println("Incorrect operands");
            System.exit(0);
        }
    }

    /** Checks out FILENAME in commit with hash CURRENT. */
    public static void checkoutFile(String filename, String current) {
        String abbrev = current.substring(0, 6);
        File toCopy = new File(
            "./.gitlet/versions/" + abbrev + "/" + filename);
        File commitWithHash = new File("./.gitlet/versions/" + abbrev);
        if (!commitWithHash.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        if (!toCopy.exists()) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            File toOverwrite = new File(filename);
            Utils.writeContents(toOverwrite, Utils.readContents(toCopy));
        }
    }

    /** Checks out all files in commit with hash HASH. Reverts state
    to earlier commit. Errors if there are untracked files that might 
    be permanently lost. */
    public static void reset(String hash) {
        try {
            Repo r = new Repo("");
            Branch rewound = r.getCurrent();
            String oldid = r.getCurrent().typableHash();
            String id = hash.substring(0, 6);
            checkForUntracked(oldid, id);
            File commitsWithId = new File("./.gitlet/metadata/" + id);
            if (!commitsWithId.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            File[] commitWithId = commitsWithId.listFiles();
            String fullID = commitWithId[0].getName();
            rewound.setSHA(fullID);
            copyFiles(oldid, id);
            rewound.store();
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Checks for files untracked by commit w/hash ID and 
    tracked by commit w/hash NEWID. */
    private static void checkForUntracked(String id, String newid) {
        id = id.substring(0, 6);
        newid = newid.substring(0, 6);
        List<String> workingFiles = Utils.plainFilenamesIn(".");
        Iterator<String> iter = workingFiles.iterator();
        String f;
        while (iter.hasNext()) {
            f = iter.next();
            File tracked = 
                new File("./.gitlet/versions/" + id + "/" + f);
            File tracked2 =
                new File("./.gitlet/versions/" + newid + "/" + f);
            if (!tracked.exists() && tracked2.exists()) {
                System.out.println(
                    "There is an untracked file in the way;"
                    + " delete it or add it first.");
                System.exit(0);
            }
        }
    }

    /** Checks out BRANCH. */
    public static void checkoutBranch(String branch) {
        Repo r = new Repo("");
        Branch former = r.getCurrent();
        String formerName = former.getName();
        File bran = new File("./.gitlet/global/" + branch);
        Branch toCheckout = loadBranch(bran);
        checkForUntracked(former.typableHash(), toCheckout.typableHash());
        if (!bran.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (toCheckout.isCurrent()) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        copyFiles(former.typableHash(), toCheckout.typableHash());
        former.leave();
        toCheckout.arrive();
        former.store();
        toCheckout.store();
    }

    /** Copies files from commit w/hash LATTER to working directory.
    Deletes files that are in working directory and FORMER but not in
    LATTER. Makes LATTER the HEAD. Clears the staging area. */
    private static void copyFiles(String former, String latter) {
        String toCopyLocation =
            "./.gitlet/versions/" + latter;
        File toCopy = new File(toCopyLocation);
        File[] files = toCopy.listFiles();
        String toOverwriteLocation =
            "./.gitlet/versions/" + former;
        File working = new File(".");
        File[] workingFiles = working.listFiles();
        for (File fi: files) {
            File copy = new File(fi.getName());
            Utils.writeContents(copy, Utils.readContents(fi));
        }
        for (File w: workingFiles) {
            File formerLocation =
                new File(toOverwriteLocation + "/" + w.getName());
            File latterLocation =
                new File(toCopyLocation + "/" + w.getName());
            if (formerLocation.exists()
                && (!latterLocation.exists())) {
                w.delete();
            }
        }
        File stagingArea = new File("./.gitlet/stage");
        File[] staged = stagingArea.listFiles();
        for (File s: staged) {
            s.delete();
        }
    }

    /** Creates a branch with name NAME. */
    public static void branch(String name) throws IOException {
        isInitialized();
        File branchDest = new File("./.gitlet/global/" + name);
        if (branchDest.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Repo dummy = new Repo("dummy");
        Branch cur = dummy.getCurrent();
        Branch branch = new Branch(name, false, cur);
        branch.store();
        cur.store();
    }

    /** Parses ARGS. Makes a new version of files, accounting for changes
    staged and removed. */
    public static void commit(String... args) {
        isInitialized();
        if ((args.length < 2)
            || (args[1].trim().equals(""))) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        } else if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else {
            Repo views = new Repo("dummy");
            Branch current = views.getCurrent();
            current.commit(args[1]);
            current.store();
            System.exit(0);
        }
    }

    /** Removes the branch with name BRANCHTORM. */
    public static void rembranch(String branchtoRm) {
        Repo r1 = new Repo("dummy");
        File target = new File("./.gitlet/global/" + branchtoRm);
        if (!target.exists()) {
            System.out.println(
                "A branch with that name does not exist.");
            System.exit(0);
        } else if (branchtoRm.equals(r1.getCurrent().getName())) {
            System.out.println("Cannot remove the current branch.");
        } else {
            target.delete();
        }
        r1.getCurrent().store();
    }

    /** Parses rm command in ARGS. */
    public static void rm(String... args) {
        isInitialized();
        twoParse(args);
        Repo rp = new Repo("dummy");
        String typable = rp.getCurrent().typableHash();
        String currentBranchName = rp.getCurrent().getName();
        rm(args[1], typable, currentBranchName);
        rp.getCurrent().store();
    }

    /** Untracks and/or unstages FILE from commit TYPABLE on branch
    CURRENTBRANCHNAME. */
    public static void rm(
        String file, String typable, String currentBranchName) {
        File toRmWorking = new File(file);
        File toRm = new File(
            "./.gitlet/versions/" + typable + "/" + file);
        File toRmStaged = new File("./.gitlet/stage/" + file);
        String placeholder = "This file is untracked";
        byte[] place = placeholder.getBytes();
        File untrack =
            new File("./.gitlet/untracked/" + currentBranchName + "/" + file);
        if (toRm.exists()) {
            Utils.writeContents(untrack, place);
            if (toRmStaged.exists()) {
                toRmStaged.delete();
            }
            toRmWorking.delete();
        } else if (toRmStaged.exists()) {
            File untold = new File(
                "./.gitlet/untold/" + currentBranchName + "/" + file);
            Utils.writeContents(untold, place);
            toRmStaged.delete();
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    /** To delete later, with a sense of accomplishment. */
    public static void dummy() {
        System.out.println("Support to follow soon. We're working on it!");
    }

    /** Prints info about branches, staged/unstaged files, removed files,
    and untracked files. */
    public static void status() {
        Repo repo = new Repo("");
        String current = repo.getCurrent().getName();
        System.out.println("=== Branches ===");
        printDir(new File("./.gitlet/global"), current);
        System.out.println("=== Staged Files ===");
        printDir(new File("./.gitlet/stage"), current);
        System.out.println("=== Removed Files ===");
        printDir(new File("./.gitlet/untracked/" + current), current);
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
    }

    /** Prints contents of a directory DIR. Prints a * by the file with
    name CURRENTBRANCH. */
    private static void printDir(File dir, String currentBranch) {
        File[] list = dir.listFiles();
        String[] listNames = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            if (list[i].getName().equals(currentBranch)) {
                listNames[i] = "*" + list[i].getName();
            } else {
                listNames[i] = list[i].getName();
            }
        }
        Arrays.parallelSort(listNames);
        for (String s: listNames) {
            System.out.println(s);
        }
        System.out.println("");
    }

    /** Prints the SHA-1 strings of all commits with message PATTERN. */
    public static void find(String pattern) {
        File meta = new File("./.gitlet/metadata");
        File[] commits = meta.listFiles();
        int i = 0;
        for (File cd: commits) {
            File[] c = cd.listFiles();
            Commit sea = loadCommit(c[0]);
            if (sea.message().equals(pattern)) {
                System.out.println(sea.getHashed());
                sea.store();
                i += 1;
                continue;
            } else {
                sea.store();
            }
        }
        if (i > 0) {
            return;
        }
        System.out.println("Found no commit with that message.");
    }

    /** Parses ARGS and prints the log for every branch. */
    public static void globalog(String... args) {
        isInitialized();
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File commits = new File("./.gitlet/metadata/");
        File[] allCommits = commits.listFiles();
        for (File c: allCommits) {
            File[] pearl = c.listFiles();
            Commit c1 = loadCommit(pearl[0]);
            System.out.println("===");
            System.out.println("Commit " + c1.getHashed());
            System.out.println(c1.getTime());
            System.out.println(c1.message());
            c1.store();
            System.out.println("");
        }
    }

    /** Prints the log for branch CURRENT. */
    public static void log(Branch current) {
        String sep1 = "===";
        LinkedList<String> history = current.getChained();
        HashMap<String, Commit> getCommits = current.load();
        String hash = current.getSHA();
        while (hash != null) {
            Commit c = getCommits.get(hash);
            System.out.println(sep1);
            System.out.println("Commit " + hash);
            System.out.println(c.getTime());
            System.out.println(c.message());
            System.out.println("");
            hash = c.parent();
            c.store();
        }
        current.store();
    }

    /** Method to add files. Parses ARGS to get the filename to stage. */
    public static void add(String... args) {
        isInitialized();
        Repo r = new Repo("");
        String head = r.getCurrent().getSHA();
        String type = r.getCurrent().typableHash();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }

        File filename = new File(args[1]);
        if (!filename.exists()) {
            System.err.println("File does not exist");
            System.exit(0);
        }
        String filenameSHA = toSHA(filename);
        String branchname = r.getCurrent().getName();

        File a = new File(".gitlet/versions/" + type + "/" + args[1]);
        if (a.exists()) {
            String aSHA = toSHA(a);
            if (aSHA.equals(filenameSHA)) {
                addRemoved(filename, branchname);
                return;
            }
        }

        File folder = new File(".gitlet/stage");
        File[] listofFiles = folder.listFiles();
        addRemoved(filename, branchname);
        if (listofFiles.length == 0) {
            stage(filename);
        }

        for (File f: listofFiles) {
            String fSHA = toSHA(f);
            if (!fSHA.equals(filenameSHA)) {
                stage(filename);
            }
        }
        r.getCurrent().store();
    }

    /** Stages file with name FILENAME. */
    private static void stage(File filename) {
        File f1 = new File("./.gitlet/stage/" + filename.getName());
        Utils.writeContents(f1, Utils.readContents(filename));
    }

    /** If applicable, un-untracks a FILENNAME untracked by branch
    BRANCHNAME. */
    public static void addRemoved(File filename, String branchname) {
        String bName = branchname;
        File wasRemoved = new File(
            "./.gitlet/untracked/" + bName + "/" + filename.getName());
        if (wasRemoved.exists()) {
            wasRemoved.delete();
            return;
        }
    }

    /** Serializing utilities. */

    /** Returns the SHA-1 string for FILE. */
    public static String toSHA(File file) {
        byte[] bites = Utils.readContents(file);
        return Utils.sha1(bites);
    }

    /** Returns the branch object serialized in file F. */
    public static Branch loadBranch(File f) {
        Branch br = null;
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(f));
            br = (Branch) in.readObject();
            in.close();
        } catch (ClassNotFoundException excp) {
            System.out.println("Bad input file");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(
                "Bad input. Encase multiple word names in quotes.");
            System.exit(0);
        }
        return br;
    }

    /** Returns a commit object serialized in file F. */
    public static Commit loadCommit(File f) {
        Commit see = null;
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(f));
            see = (Commit) in.readObject();
            in.close();
        } catch (ClassNotFoundException excp) {
            System.out.println("Bad input file");
        } catch (IOException e) {
            System.out.println("Bad input.");
        }
        return see;
    }
}
