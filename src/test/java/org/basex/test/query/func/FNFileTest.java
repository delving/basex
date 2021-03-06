package org.basex.test.query.func;

import static org.basex.query.func.Function.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.basex.core.*;
import org.basex.io.*;
import org.basex.query.util.*;
import org.basex.test.query.*;
import org.junit.*;

/**
 * This class tests the functions of the file library.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Rositsa Shadura
 */
public final class FNFileTest extends AdvancedQueryTest {
  /** Test path. */
  private static final String PATH = Prop.TMP + NAME + '/';
  /** Test path. */
  private static final String PATH1 = PATH + NAME;
  /** Test path. */
  private static final String PATH2 = PATH + NAME + '2';
  /** Test path. */
  private static final String PATH3 = PATH + NAME + "/x";
  /** Test path. */
  private static final String PATH4 = PATH + NAME + "/x/y";

  /** Initializes the test. */
  @After
  public void init() {
    new File(PATH4).delete();
    new File(PATH3).delete();
    new File(PATH2).delete();
    new File(PATH1).delete();
  }

  /**
   * Test method for the dir-separator() function.
   */
  @Test
  public void fileDirSeparator() {
    check(_FILE_DIR_SEPARATOR);
    assertTrue(!query(_FILE_DIR_SEPARATOR.args()).isEmpty());
  }

  /**
   * Test method for the path-separator() function.
   */
  @Test
  public void filePathSeparator() {
    check(_FILE_PATH_SEPARATOR);
    assertTrue(!query(_FILE_PATH_SEPARATOR.args()).isEmpty());
  }

  /**
   * Test method for the line-separator() function.
   */
  @Test
  public void fileLineSeparator() {
    check(_FILE_LINE_SEPARATOR);
  }

  /**
   * Test method for the exists() function.
   */
  @Test
  public void fileExists() {
    check(_FILE_EXISTS);
    query(_FILE_WRITE.args(PATH1, "()"));
    query(_FILE_EXISTS.args(PATH1), true);
    query(_FILE_EXISTS.args(IO.FILEPREF + "//" + PATH1), true);
    query(_FILE_DELETE.args(PATH1));
    query(_FILE_EXISTS.args(PATH1), false);
    query(_FILE_EXISTS.args(IO.FILEPREF + "//" + PATH1), false);
  }

  /**
   * Test method for the is-dir() function.
   */
  @Test
  public void fileIsDir() {
    check(_FILE_IS_DIR);
    query(_FILE_IS_DIR.args(PATH), true);
    query(_FILE_WRITE.args(PATH1, "()"));
    query(_FILE_IS_DIR.args(PATH1), false);
    query(_FILE_DELETE.args(PATH1));
    query(_FILE_CREATE_DIR.args(PATH1));
    query(_FILE_IS_DIR.args(PATH1), true);
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the is-file() function.
   */
  @Test
  public void fileIsFile() {
    check(_FILE_IS_FILE);
    query(_FILE_IS_FILE.args(PATH), false);
    query(_FILE_WRITE.args(PATH1, "()"));
    query(_FILE_IS_FILE.args(PATH1), true);
    query(_FILE_DELETE.args(PATH1));
    query(_FILE_CREATE_DIR.args(PATH1));
    query(_FILE_IS_FILE.args(PATH1), false);
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the last-modified() function.
   */
  @Test
  public void fileLastModified() {
    check(_FILE_LAST_MODIFIED);
    assertTrue(!query(_FILE_LAST_MODIFIED.args(PATH)).isEmpty());
  }

  /**
   * Test method for the size() function.
   */
  @Test
  public void fileSize() {
    check(_FILE_SIZE);
    query(_FILE_WRITE.args(PATH1, "abcd"));
    query(_FILE_SIZE.args(PATH1), "4");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the list() function.
   */
  @Test
  public void fileList() {
    check(_FILE_LIST);
    error(_FILE_LIST.args(PATH1), Err.FILE_NODIR);
    query(_FILE_WRITE.args(PATH1, "()"));
    error(_FILE_LIST.args(PATH1), Err.FILE_NODIR);
    contains(_FILE_LIST.args(PATH), NAME);
    contains(_FILE_LIST.args(PATH, "false()"), NAME);
    contains(_FILE_LIST.args(PATH, "false()", "Sandbox"), NAME);
    contains(_FILE_LIST.args(PATH, "false()", NAME), NAME);
    query(_FILE_LIST.args(PATH, "false()", "XXX"), "");
    query(_FILE_DELETE.args(PATH1));
    // check recursive paths
    query(_FILE_CREATE_DIR.args(PATH1));
    query(_FILE_CREATE_DIR.args(PATH3));
    query(_FILE_WRITE.args(PATH4, "()"));
    contains(_FILE_LIST.args(PATH1, "true()"), "y");
  }

  /**
   * Test method for the create-dir() function.
   */
  @Test
  public void fileCreateDir() {
    check(_FILE_CREATE_DIR);
    query(_FILE_CREATE_DIR.args(PATH1));
    query(_FILE_CREATE_DIR.args(PATH1));
    query(_FILE_CREATE_DIR.args(PATH3));
    query(_FILE_DELETE.args(PATH1, "true()"));
    query(_FILE_WRITE.args(PATH1, "()"));
    error(_FILE_CREATE_DIR.args(PATH1), Err.FILE_EXISTS);
    error(_FILE_CREATE_DIR.args(PATH3), Err.FILE_EXISTS);
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the delete() function.
   */
  @Test
  public void fileDelete() {
    check(_FILE_DELETE);
    query(_FILE_CREATE_DIR.args(PATH3));
    query(_FILE_DELETE.args(PATH3));
    query(_FILE_CREATE_DIR.args(PATH3));
    query(_FILE_WRITE.args(PATH4, "()"));
    query(_FILE_DELETE.args(PATH1, "true()"));
    error(_FILE_DELETE.args(PATH1), Err.FILE_WHICH);
  }

  /**
   * Test method for the read-text() function.
   */
  @Test
  public void fileReadText() {
    check(_FILE_READ_TEXT);
    error(_FILE_READ_TEXT.args(PATH1), Err.FILE_WHICH);
    error(_FILE_READ_TEXT.args(PATH), Err.FILE_DIR);
    query(_FILE_WRITE.args(PATH1, "a\u00e4"));
    query(_FILE_READ_TEXT.args(PATH1), "a\u00e4");
    error(_FILE_READ_TEXT.args(PATH1, "UNKNOWN"), Err.FILE_ENCODING);
    assertEquals(3, query(_FILE_READ_TEXT.args(PATH1, "CP1252")).length());
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the read-binary() function.
   */
  @Test
  public void fileReadBinary() {
    check(_FILE_READ_BINARY);
    error(_FILE_READ_BINARY.args(PATH1), Err.FILE_WHICH);
    error(_FILE_READ_BINARY.args(PATH), Err.FILE_DIR);
    query(_FILE_WRITE.args(PATH1, "0"));
    query(_FILE_READ_BINARY.args(PATH1), "MA==");
    query(_FILE_WRITE.args(PATH1, "a\u00e4"));
    query(_FILE_READ_BINARY.args(PATH1), "YcOk");
    query(_FILE_WRITE_BINARY.args(PATH1, _CONVERT_STRING_TO_BASE64.args("a\u00e4")));
    query(_FILE_READ_BINARY.args(PATH1), "YcOk");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the write() function.
   */
  @Test
  public void fileWrite() {
    check(_FILE_WRITE);

    error(_FILE_WRITE.args(PATH, "()"), Err.FILE_DIR);
    error(_FILE_WRITE.args(PATH4, "()"), Err.FILE_NODIR);

    query(_FILE_WRITE.args(PATH1, "0"));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_WRITE.args(PATH1, "0"));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_DELETE.args(PATH1));

    query(_FILE_WRITE.args(PATH1, "a\u00e4",
        serialParams("<encoding value='CP1252'/>")));
    query(_FILE_READ_TEXT.args(PATH1, "CP1252"), "a\u00e4");

    query(_FILE_WRITE.args(PATH1, "\"<a/>\"",
        serialParams("<method value='text'/>")));
    query(_FILE_READ_TEXT.args(PATH1), "&lt;a/&gt;");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the append() function.
   */
  @Test
  public void fileAppend() {
    check(_FILE_APPEND);

    error(_FILE_APPEND.args(PATH, "()"), Err.FILE_DIR);
    error(_FILE_APPEND.args(PATH4, "()"), Err.FILE_NODIR);

    query(_FILE_APPEND.args(PATH1, "0"));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_APPEND.args(PATH1, "0", "()"));
    query(_FILE_SIZE.args(PATH1), "2");
    query(_FILE_DELETE.args(PATH1));

    query(_FILE_APPEND.args(PATH1, "a\u00e4",
        serialParams("<encoding value='CP1252'/>")));
    query(_FILE_READ_TEXT.args(PATH1, "CP1252"), "a\u00e4");
    query(_FILE_DELETE.args(PATH1));

    query(_FILE_APPEND.args(PATH1, "\"<a/>\"",
        serialParams("<method value='text'/>")));
    query(_FILE_READ_TEXT.args(PATH1), "&lt;a/&gt;");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the write-text() function.
   */
  @Test
  public void fileWriteText() {
    check(_FILE_WRITE_TEXT);

    error(_FILE_WRITE_TEXT.args(PATH, "x"), Err.FILE_DIR);
    error(_FILE_WRITE_TEXT.args(PATH1, " 123"), Err.XPTYPE);

    query(_FILE_WRITE_TEXT.args(PATH1, "x"));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_WRITE_TEXT.args(PATH1, "\u00fc", "US-ASCII"));
    query(_FILE_READ_TEXT.args(PATH1), "?");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the write-text-lines() function.
   */
  @Test
  public void fileWriteTextLines() {
    check(_FILE_WRITE_TEXT_LINES);

    error(_FILE_WRITE_TEXT_LINES.args(PATH, "x"), Err.FILE_DIR);
    error(_FILE_WRITE_TEXT_LINES.args(PATH1, " 123"), Err.XPTYPE);

    query(_FILE_WRITE_TEXT_LINES.args(PATH1, "x"));
    query(_FILE_SIZE.args(PATH1), 1 + Prop.NL.length());
    query(_FILE_WRITE_TEXT_LINES.args(PATH1, "\u00fc", "US-ASCII"));
    query(_FILE_READ_TEXT_LINES.args(PATH1), "?");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the write-binary() function.
   */
  @Test
  public void fileWriteBinary() {
    check(_FILE_WRITE_BINARY);

    final String bin = "xs:base64Binary('MA==')";
    error(_FILE_WRITE_BINARY.args(PATH, bin), Err.FILE_DIR);
    error(_FILE_WRITE_BINARY.args(PATH1, "NoBinary"), Err.BINARYTYPE);

    query(_FILE_WRITE_BINARY.args(PATH1, bin));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_WRITE_BINARY.args(PATH1, bin));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the append-binary() function.
   */
  @Test
  public void fileAppendBinary() {
    check(_FILE_APPEND_BINARY);

    final String bin = "xs:base64Binary('MA==')";
    error(_FILE_APPEND_BINARY.args(PATH, bin), Err.FILE_DIR);
    error(_FILE_WRITE_BINARY.args(PATH1, "NoBinary"), Err.BINARYTYPE);

    query(_FILE_APPEND_BINARY.args(PATH1, bin));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_APPEND_BINARY.args(PATH1, bin));
    query(_FILE_READ_TEXT.args(PATH1), "00");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the append-text() function.
   */
  @Test
  public void fileAppendText() {
    check(_FILE_APPEND_TEXT);

    error(_FILE_APPEND_TEXT.args(PATH, "x"), Err.FILE_DIR);
    error(_FILE_APPEND_TEXT.args(PATH1, " 123"), Err.XPTYPE);

    query(_FILE_APPEND_TEXT.args(PATH1, "x"));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_APPEND_TEXT.args(PATH1, "\u00fc", "US-ASCII"));
    query(_FILE_READ_TEXT.args(PATH1), "x?");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the append-text-lines() function.
   */
  @Test
  public void fileAppendTextLines() {
    check(_FILE_APPEND_TEXT_LINES);

    error(_FILE_APPEND_TEXT_LINES.args(PATH, "x"), Err.FILE_DIR);
    error(_FILE_APPEND_TEXT_LINES.args(PATH1, " 123"), Err.XPTYPE);

    query(_FILE_APPEND_TEXT_LINES.args(PATH1, "x"));
    query(_FILE_SIZE.args(PATH1), 1 + Prop.NL.length());
    query(_FILE_APPEND_TEXT_LINES.args(PATH1, "('y','z')"));
    query(_FILE_SIZE.args(PATH1), 3 * (1 + Prop.NL.length()));
    query(_FILE_APPEND_TEXT_LINES.args(PATH1, "\u00fc", "US-ASCII"));
    query(_FILE_READ_TEXT_LINES.args(PATH1), "x y z ?");
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the copy() function.
   */
  @Test
  public void fileCopy() {
    check(_FILE_COPY);

    query(_FILE_WRITE.args(PATH1, "A"));
    query(_FILE_COPY.args(PATH1, PATH2));
    query(_FILE_COPY.args(PATH1, PATH2));
    query(_FILE_COPY.args(PATH2, PATH2));
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_SIZE.args(PATH2), "1");
    error(_FILE_COPY.args(PATH1, PATH3), Err.FILE_NODIR);

    query(_FILE_DELETE.args(PATH1));
    query(_FILE_DELETE.args(PATH2));
  }

  /**
   * Test method for the move() function.
   */
  @Test
  public void fileMove() {
    check(_FILE_MOVE);

    error(_FILE_MOVE.args(PATH1, PATH2), Err.FILE_WHICH);
    query(_FILE_WRITE.args(PATH1, "a"));
    query(_FILE_MOVE.args(PATH1, PATH2));
    query(_FILE_MOVE.args(PATH2, PATH1));
    query(_FILE_MOVE.args(PATH1, PATH1));
    error(_FILE_MOVE.args(PATH1, PATH4), Err.FILE_NODIR);
    query(_FILE_SIZE.args(PATH1), "1");
    query(_FILE_EXISTS.args(PATH2), false);
    query(_FILE_DELETE.args(PATH1));
  }

  /**
   * Test method for the resolve-path() function.
   */
  @Test
  public void fileResolvePath() {
    check(_FILE_RESOLVE_PATH);
    final String path = query(_FILE_RESOLVE_PATH.args(PATH1));
    final String can = new File(PATH1).getAbsolutePath();
    assertEquals(path.toLowerCase(Locale.ENGLISH), can.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Test method for the path-to-uri() function.
   */
  @Test
  public void filePathToURI() {
    check(_FILE_PATH_TO_URI);
    final String path = query(_FILE_PATH_TO_URI.args(PATH1));
    final String uri = new File(PATH1).toURI().toString();
    assertEquals(path.toLowerCase(Locale.ENGLISH), uri.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Tests method for base-name() function.
   */
  @Test
  public void fileBaseName() {
    check(_FILE_BASE_NAME);

    // check with a simple path
    query(_FILE_BASE_NAME.args(PATH1), NAME);
    // check with a path ending with a directory separator
    query(_FILE_BASE_NAME.args(PATH1 + File.separator), NAME);
    // check with a path consisting only of directory separators
    query(_FILE_BASE_NAME.args("//"), "");
    // check with empty string path
    query(_FILE_BASE_NAME.args(""), ".");
    // check using a suffix
    query(_FILE_BASE_NAME.args(PATH1 + File.separator + "test.xml", ".xml"), "test");
  }

  /**
   * Tests method for dir-name() function.
   */
  @Test
  public void fileDirName() {
    check(_FILE_DIR_NAME);
    // check with a simple path
    assertEquals(norm(PATH),
        norm(query(_FILE_DIR_NAME.args(PATH1))).toLowerCase(Locale.ENGLISH));
    // check with an empty path
    query(_FILE_DIR_NAME.args(""), ".");
    // check with a path without directory separators
    query(_FILE_DIR_NAME.args(NAME), ".");
  }

  /**
   * Tests method for path-to-native() function.
   * @throws IOException I/O exception
   */
  @Test
  public void filePathToNative() throws IOException {
    check(_FILE_PATH_TO_NATIVE);
    assertEquals(norm(new File(PATH1).getCanonicalPath()),
        norm(query(_FILE_PATH_TO_NATIVE.args(PATH1))));
    query(_FILE_PATH_TO_NATIVE.args(PATH + ".." + "/test.xml"),
        new File(PATH + ".." + "/test.xml").getCanonicalPath());
  }

  /**
   * Normalize slashes of specified path to reduce OS dependent bugs.
   * @param path input path
   * @return normalized path
   */
  private static String norm(final String path) {
    return (path + '/').replaceAll("[\\\\/]+", "/").toLowerCase(Locale.ENGLISH);
  }
}
