package com.langmuir.util.nest.jenetics_with_NFP;

import com.langmuir.util.nest.config.Config;
import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.data.Segment;
import com.langmuir.util.nest.util.CommonUtil;
import com.langmuir.util.nest.util.GeometryUtil;
import java.util.List;

public class Util {

  /**
   * translate bin to the origin and ensure it can be used for nesting
   *
   * @param bin Nespath to clean
   * @return Nestpath cleaned
   */
  public static NestPath cleanBin(NestPath bin) {

    NestPath binPolygon = CommonUtil.cleanNestPath(bin);
    if (Config.BOUND_SPACING > 0) {
      List<NestPath> offsetBin = CommonUtil.polygonOffset(binPolygon, -Config.BOUND_SPACING);
      if (offsetBin.size() == 1)
        binPolygon = offsetBin.getFirst();
    }

    binPolygon.setId(-1);
    double xbinmax = binPolygon.getSegment(0).x;
    double xbinmin = binPolygon.getSegment(0).x;
    double ybinmax = binPolygon.getSegment(0).y;
    double ybinmin = binPolygon.getSegment(0).y;
    // Find min max
    for (int i = 1; i < binPolygon.size(); i++) {
      if (binPolygon.getSegment(i).x > xbinmax) {
        xbinmax = binPolygon.getSegment(i).x;
      } else if (binPolygon.getSegment(i).x < xbinmin) {
        xbinmin = binPolygon.getSegment(i).x;
      }

      if (binPolygon.getSegment(i).y > ybinmax) {
        ybinmax = binPolygon.getSegment(i).y;
      } else if (binPolygon.getSegment(i).y < ybinmin) {
        ybinmin = binPolygon.getSegment(i).y;
      }
    }

    /*binpath is translated to origin*/
    for (int i = 0; i < binPolygon.size(); i++) {
      binPolygon.getSegment(i).x -= xbinmin;
      binPolygon.getSegment(i).y -= ybinmin;
    }

    if (GeometryUtil.polygonArea(binPolygon) > 0)
      binPolygon.reverse();

    return binPolygon;
  }

  /**
   * Ensure every polygon can be nested
   *
   * @param tree List<NestPath> to be clean
   */
  public static void cleanTree(List<NestPath> tree) {
    for (NestPath element : tree) {
      Segment start = element.getSegment(0);
      Segment end = element.getSegment(element.size() - 1);
      if (start == end || GeometryUtil.almostEqual(start.x, end.x) && GeometryUtil.almostEqual(start.y, end.y)) {
        element.removeLastSegment();
      }
      if (GeometryUtil.polygonArea(element) > 0)
        element.reverse();
    }

  }

  /**
   * Return a rectangle NestPath starting in origin with width and height specified
   *
   * @param width
   * @param height
   * @return NestPath
   * @author Alberto Gambarara
   */
  public static NestPath createRectPolygon(double width, double height) {
    NestPath np = new NestPath();
    np.add(0, 0);
    np.add(width, 0);
    np.add(width, height);
    np.add(0, height);
    return np;

  }

}
