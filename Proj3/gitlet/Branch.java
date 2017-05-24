package gitlet;

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/** Keeps track of changes produced by one person.
@author Andrew Leong & Theo Joseph
*/

public class Branch implements Serializable {

    /** Creates a new branch with name LABEL. ISCURR says whether the branch
    is the current one in the repo, false by default. */
    Branch(String label, boolean isCurr) {
        try {
            _name = label;
            _iscurr = isCurr;
            _chain = new LinkedList<String>();
            commit("initial commit");
            Files.createDirectory(
                Paths.get(".", ".gitlet", "untracked", _name));
            Files.createDirectory(Paths.get(".", ".gitlet", "untold", _name));
        } catch (IOException excp) {
            System.out.println("Could not write file.");
            System.exit(0);
        }
    }

    /** Creates a new branch with new name LABEL and state initialized
    to branch COPYFROM, ISCURR initialized to FALSE. */
    Branch(String label, boolean isCurr, Branch copyFrom) {
        try {
            _name = label;
            _iscurr = isCurr;
            _chain = copyFrom.getChained();
            _head = copyFrom._head;
            Files.createDirectory(
                Paths.get(".", ".gitlet", "untracked", _name));
            Files.createDirectory(Paths.get(".", ".gitlet", "untold", _name));
        } catch (IOException e) {
            System.out.println("File creation unsuccessful.");
            System.exit(0);
        }
    }

    /** Returns whether this branch is the current branch. */
    boolean isCurrent() {
        return _iscurr;
    }

    /** Leave this branch for another one. */
    void leave() {
        _iscurr = false;
    }

    /** Makes this branch the current branch. */
    void arrive() {
        _iscurr = true;
    }

    /** Returns a runtime map between SHA-1 strings and commit objects. */
    HashMap<String, Commit> load() {
        HashMap<String, Commit> map = new HashMap<String, Commit>();
        File branchCommits = new File("./.gitlet/metadata/");
        File[] treeOfCommitment = branchCommits.listFiles();
        try {
            for (File c: treeOfCommitment) {
                File[] c0 = c.listFiles();
                ObjectInputStream in =
                    new ObjectInputStream(new FileInputStream(c0[0]));
                Commit commit = (Commit) in.readObject();
                String sha = commit.getHashed();
                map.put(sha, commit);
            }
        } catch (FileNotFoundException excp) {
            System.out.println("File not found.");
            System.exit(0);
        } catch (ClassNotFoundException ex) {
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Bad input.");
            System.exit(0);
        }
        return map;
    }

    /** Make a new commit with message MSG. */
    void commit(String msg) {
        try {
            Commit c = new Commit(msg, _head, _name);
            _head = c.getHashed();
            _chain.add(_head);
            Path commitsForBranch =
                Paths.get(".", ".gitlet", "metadata", typableHash());
            if (!Files.exists(commitsForBranch)) {
                commitsForBranch = Files.createDirectories(commitsForBranch);
            }
            c.store();
        } catch (IOException excp) {
            System.out.println("Could not read file. Sorry!");
            System.exit(0);
        }
    }

    /** Re-serializes updated branch object. */
    void store() {
        try {
            File branchFile = new File("./.gitlet/global/" + _name);
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(branchFile));
            out.writeObject(this);
            out.close();
        } catch (IOException e) {
            System.out.println("Something went wrong.");
        }
    }

    /** Returns present commit. */
    Commit getHead() {
        HashMap<String, Commit> map = load();
        return map.get(_head);
    }

    /** Sets _head to SHA-1 string HEAD. */
    void setSHA(String head) {
        _head = head;
    }

    /** Returns SHA-1 string of present commit. */
    String getSHA() {
        return _head;
    }

    /** Returns first 6 characters of _head. */
    String typableHash() {
        return _head.substring(0, 6);
    }

    /** Returns commit tree of SHA-1's. */
    LinkedList<String> getChained() {
        return _chain;
    }

    /** Returns the branch's name. */
    String getName() {
        return _name;
    }

    /** The name of the branch. */
    private String _name;
    /** SHA-1 string of the commit (state) of the branch .*/
    private String _head;
    /** Says whether this is the current branch or not. */
    private boolean _iscurr;
    /** The tree of all previous commits. */
    private LinkedList<String> _chain;
}
