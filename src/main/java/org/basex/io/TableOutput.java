package org.basex.io;

import java.io.FileOutputStream;
import java.io.IOException;
import org.basex.data.MetaData;
import org.basex.util.IntList;

/**
 * This class allows a blockwise output of the database table.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 * @author Tim Petrowsky
 */
public final class TableOutput extends FileOutputStream {
  /** Buffer. */
  private final byte[] buffer = new byte[IO.BLOCKSIZE];
  /** Index entries. */
  private final IntList firstPres = new IntList();
  /** Index entries. */
  private final IntList blocks = new IntList();

  /** Meta data. */
  private final MetaData meta;
  /** Current filename. */
  private final String file;

  /** Position inside buffer. */
  private int pos;
  /** Block count. */
  private int bcount;
  /** First pre value of current block. */
  private int fpre;
  /** Closed flag. */
  private boolean closed;

  /**
   * Initializes the output.
   * The database suffix will be added to all filenames.
   * @param md meta data
   * @param fn the file to be written to
   * @throws IOException IO Exception
   */
  public TableOutput(final MetaData md, final String fn) throws IOException {
    super(md.file(fn));
    meta = md;
    file = fn;
  }

  @Override
  public void write(final int b) throws IOException {
    if(pos == IO.BLOCKSIZE) flush();
    buffer[pos++] = (byte) b;
  }

  @Override
  public void flush() throws IOException {
    if(pos == 0) return;
    super.write(buffer);
    firstPres.add(fpre);
    blocks.add(bcount++);
    fpre += pos >>> IO.NODEPOWER;
    pos = 0;
  }

  @Override
  public void close() throws IOException {
    // close() is called by super.finalize().
    // The flag prevents that the file is closed more than once.
    if(closed) return;
    closed = true;

    flush();
    super.close();

    final DataOutput info = new DataOutput(meta.file(file + 'i'));
    info.writeNum(bcount);
    info.writeNum(bcount);
    info.writeNums(firstPres.toArray());
    info.writeNums(blocks.toArray());
    info.close();
  }
}
