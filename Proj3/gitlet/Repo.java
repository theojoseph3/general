package gitlet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.io.File;
import java.io.Serializable;
/** A view of a repository. Contains a pointer to the current branch, which
will keep track of commit trees and whatnot. Also contains a log of all changes
on all branches.
>>>>>>> partner/proj3
@author Andrew Leong & Khalil Joseph
*/
public class Repo implements Serializable {

    /** Initializes a .gitlet directory. */
    Repo() {
        try {
            Path director = Paths.get(".", ".gitlet");
            Path mkdir = Files.createDirectory(director);
            Path staging =
                Files.createDirectory(Paths.get(".", ".gitlet", "stage"));
            Path removed =
                Files.createDirectory(Paths.get(".", ".gitlet", "untracked"));
            Path untracked =
                Files.createDirectory(Paths.get(".", ".gitlet", "untold"));
            Path allBranches =
                Files.createDirectory(Paths.get(".", ".gitlet", "global"));
            Path metadata =
                Files.createDirectory(Paths.get(".", ".gitlet", "metadata"));
            Path allFiles =
                Files.createDirectory(Paths.get(".", ".gitlet", "versions"));
            Path temp =
                Files.createDirectory(
                    Paths.get(".", ".gitlet", "versions", "temp"));
            File master = new File("./.gitlet/global/master.ser");
            Branch initial = new Branch("master", true);
            initial.store();
        } catch (FileAlreadyExistsException excp) {
            System.out.println(
                "A gitlet version-control system already exists in the "
                + "current directory.");
        } catch (IOException e) {
            System.out.println("I/O troubles; please try again.");
            System.exit(0);
        }
    }

    /** Loads an internal repo object to represent the current state
    of the repository. */
    Repo(String dummy) {
        File global = new File("./.gitlet/global");
        File[] branches = global.listFiles();
        for (File b: branches) {
            Branch br = Main.loadBranch(b);
            if (br.isCurrent()) {
                current = br;
            } else {
                br.store();
            }
        }
    }


    /** Returns the current branch. */
    Branch getCurrent() {
        return current;
    }

    /** Branch we're on. */
    private Branch current;
}
