package org.basex.gui.view.tree;

import org.basex.core.Context;
import org.basex.data.Data;

/**
 * This class stores the subtrees.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Wolfgang Miller
 */
public class TreeSubtree {
  /** TreeNodeCache Object, contains cached document. */
  private final TreeNodeCache nc;
  /** Subtree borders. */
  private TreeBorder[][] border;

  /**
   * Creates new subtree.
   * @param d data
   */
  TreeSubtree(final Data d) {
    nc = new TreeNodeCache(d);
  }

  /**
   * Generate subtree borders.
   * @param c context
   * @return root length
   */
  int generateBorders(final Context c) {
    final Data d = c.current.data;
    final int[] roots = c.current.nodes;
    final int rl = roots.length;
    if(rl == 0) return rl;
    border = new TreeBorder[rl][];

    for(int i = 0; i < rl; i++) {
      border[i] = nc.subtree(d, roots[i]);
    }
    return rl;
  }

  /**
   * Returns pre by given index.
   * @param rn root
   * @param lv level
   * @param ix index
   * @return pre
   */
  int getPrePerIndex(final int rn, final int lv, final int ix) {
    return getPrePerIndex(getTreeBorder(rn, lv), ix);
  }

  /**
   * Returns pre by given index.
   * @param bo border
   * @param ix index
   * @return pre
   */
  private int getPrePerIndex(final TreeBorder bo, final int ix) {
    return nc.getPrePerLevelAndIndex(bo.level, bo.start + ix);
  }

  /**
   * Returns index of pre.
   * @param rn root
   * @param lv level
   * @param pre pre
   * @return index
   */
  int getPreIndex(final int rn, final int lv, final int pre) {
    return getPreIndex(getTreeBorder(rn, lv), pre);
  }

  /**
   * Returns index of pre.
   * @param bo border
   * @param pre pre
   * @return index
   */
  private int getPreIndex(final TreeBorder bo, final int pre) {
    return nc.searchPreIndex(bo.level, pre, pre, bo.start, bo.getEnd())
        - bo.start;
  }

  /**
   * Returns level size.
   * @param rn root
   * @param lv level
   * @return size
   */
  int getLevelSize(final int rn, final int lv) {
    return getTreeBorder(rn, lv).size;
  }

  /**
   * Returns TreeBorder.
   * @param rn root
   * @param lv level
   * @return TreeBorder
   */
  TreeBorder getTreeBorder(final int rn, final int lv) {
    return border[rn][lv];
  }

  /**
   * Returns subtree height.
   * @param rn root
   * @return height
   */
  int getSubtreeHeight(final int rn) {
    return border.length > rn ? border[rn].length : -1;
  }

  /**
   * Returns maximum subtree height.
   * @return max height
   */
  int getMaxSubtreeHeight() {
    int h = 0;
    for(int i = 0; i < border.length; i++) {
      final int hh = border[i].length;
      if(hh > h) h = hh;
    }
    return h;
  }

  /**
   * Determines the index position of given pre value.
   * @param rn root
   * @param lv level
   * @param pre pre value
   * @return the determined index position
   */
  int searchPreArrayPos(final int rn, final int lv, final int pre) {
    return searchPreArrayPos(getTreeBorder(rn, lv), pre);
  }

  /**
   * Determines the index position of given pre value.
   * @param bo border
   * @param pre pre value
   * @return the determined index position
   */
  private int searchPreArrayPos(final TreeBorder bo, final int pre) {
    return nc.searchPreArrayPos(bo.level, bo.start, bo.getEnd(), pre)
        - bo.start;
  }

  /**
   * Returns subtree borders.
   * @param d data
   * @param pre pre
   * @return subtree borders
   */
  TreeBorder[] subtree(final Data d, final int pre) {
    return nc.subtree(d, pre);
  }
}