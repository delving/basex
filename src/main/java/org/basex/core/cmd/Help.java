package org.basex.core.cmd;

import static org.basex.core.Text.*;

import java.io.*;

import org.basex.core.*;
import org.basex.core.parse.Commands.*;
import org.basex.util.list.*;

/**
 * Evaluates the 'help' command and returns help on the database commands.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class Help extends Command {
  /**
   * Default constructor.
   * @param arg optional argument
   */
  public Help(final String arg) {
    super(Perm.NONE, arg);
  }

  @Override
  protected boolean run() throws IOException {
    final String key = args[0];
    if(key != null && !key.isEmpty()) {
      final Cmd cmd = getOption(key, Cmd.class);
      if(cmd == null) return error(UNKNOWN_CMD_X, this);
      out.print(cmd.help(true));
    } else {
      out.println(TRY_SPECIFIC_X);
      for(final Cmd c : Cmd.values()) out.print(c.help(false));
    }
    return true;
  }

  @Override
  protected boolean databases(final StringList db) {
    return true;
  }
}
