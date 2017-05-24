package gitlet;

import ucb.junit.textui;
import java.io.File;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  To be honest, I really didn't bother with much unit testing at all.
 *  The bulk of my stuff is integration tests.
 *  @author Andrew Leong
 */
public class UnitTest {

    /** Run the JUnit tests in the gitlet package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** Checks the right files are being staged. */
    @Test
    public void addTest() throws IOException {
        File gitlet = new File("./.gitlet");
        if (gitlet.exists()) {
            gitlet.delete();
        }
        Repo r = new Repo();
        Main.add("add", "eumaeus.txt");
        File staging = new File("./.gitlet/stage");
        File[] stage = staging.listFiles();
        assertTrue(stage.length == 1);
        Main.add("add", "eumaeus.txt");
        assertTrue(stage.length == 1);
    }

    /** Checks that files are being committed. */
    @Test
    public void commitTest() {
        Repo r = new Repo("");
        Main.commit("commit", "woo!");
        File versions = new File("./.gitlet/versions");
        File[] version = versions.listFiles();
        assertTrue(version.length == 2);
        String hashofwoo = r.getCurrent().typableHash();
        File backup = new File("./.gitlet/versions" + hashofwoo);
        File[] backupfiles = backup.listFiles();
        assertTrue(backupfiles.length == 1);
    }

    /** Makes sure untracked files aren't being committed unless
    added, modified, added again, and committed. */
    @Test
    public void breakCommitTest() {
        File staging = new File("./.gitlet/stage");
        assertEquals(staging.listFiles().length, 0);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {
    }

    /** A test to check whether it initializes correctly. **/
    @Test
    public void initTest() {

    }

}


