package gitlet;
import java.io.File;
import java.util.HashSet;
import java.io.Serializable;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/** A commit represents the state of a group of blobs at a given time.
At all times there is a notion of a branch of previous commits, or parents.
Each commit has a message, references to blobs, and a unique SHA-1 string
and timestamp.
@author Andrew Leong & Khalil Joseph
*/

public class Commit implements Serializable {

    /** Creates a new commit with message MESSAGE, parent with SHA-1 PARENT,
    initialized with BRANCH pointing to it. */
    Commit(String message, String parent, String branch) throws IOException {
        _parent = parent;
        _msg = message;
        _branchName = branch;
        Date date = new Date();
        DateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        _timestamp = timestamp.format(new Date());
        Path commitSub;
        File allWorld = new File("./.gitlet/stage");
        File allDels = new File("./.gitlet/untracked/" + _branchName);
        File[] toCommit = allWorld.listFiles();
        File[] toDel = allDels.listFiles();
        if ((toCommit.length == 0)
            && (parent != null)
            && (toDel.length == 0)) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else if (parent == null) {
            _hash = hash(toCommit);
            typable = _hash.substring(0, 6);
            Path sub = Files.createDirectory(
                Paths.get(".", ".gitlet", "versions", typable));
            moveFrom(toCommit, "versions/" + typable, true);
        } else {
            String shortHash = _parent.substring(0, 6);
            File previousCommit =
                new File("./.gitlet/versions/" + shortHash);
            File[] oldFiles = previousCommit.listFiles();
            File fi = new File("./.gitlet/versions/temp");
            _hash = "temp";
            moveFrom(oldFiles, "versions/temp", false);
            moveFrom(toCommit, "versions/temp", true);
            _hash = hash(fi.listFiles());
            typable = _hash.substring(0, 6);
            commitSub = Files.createDirectory(
                Paths.get(".", ".gitlet", "versions", typable));
            moveFrom(
                fi.listFiles(), "versions/" + typable, true);
            File doNotTrack =
                new File("./.gitlet/untracked/" + _branchName);
            File[] untracked = doNotTrack.listFiles();
            for (File u: untracked) {
                u.delete();
            }
        }
    }

    /** Move a list SOURCE of files in one directory to directory PATH.
    If DESTRUCTIVE, destroys the original copy. */
    public void moveFrom(File[] source, String path,
                                    boolean destructive) {
        for (File s: source) {
            File untracked = new File(
                "./.gitlet/untracked/" + _branchName + "/" + s.getName());
            if (!untracked.exists()) {
                File copy = new File("./.gitlet/" + path + "/" + s.getName());
                Utils.writeContents(copy, Utils.readContents(s));
                if (destructive) {
                    s.delete();
                }
            }
        }
    }

    /** Re-serializes this commit. */
    void store() {
        try {
            File metadata = new File(
                "./.gitlet/metadata/" + typable + "/" + _hash);
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(metadata));
            out.writeObject(this);
            out.close();
        } catch (IOException e) {
            System.out.println("Something went wrong.");
        }
    }

    /** Returns the SHA-1 string for a commit containing CONTENTS. */
    String hash(File[] contents) {
        ArrayList<Object> concats = new ArrayList<Object>();
        for (File c: contents) {
            concats.add(Utils.readContents(c));
        }
        if (_parent != null) {
            concats.add(_parent);
        }
        concats.add(_msg);
        concats.add(_timestamp.toString());
        return Utils.sha1(concats);
    }

    /** Returns the SHA-1 string for a commit. */
    String getHashed() {
        return _hash;
    }

    /** Returns the string's parent. */
    String parent() {
        return _parent;
    }
    /** Returns the timestamp for this commit. */
    String getTime() {
        return _timestamp;
    }

    /** Returns the commit message. */
    String message() {
        return _msg;
    }

    /** SHA-1 string for this commit. */
    private String _hash;
    /** First 6 characters of the hash. */
    private String typable;
    /** SHA-1 string of parent of the commit (null if the initial commit). */
    private String _parent;
    /** Date and time of the commit. */
    private String _timestamp;
    /** Commit message. */
    private String _msg;
    /** Branch this commit belongs to. */
    private String _branchName;
}

