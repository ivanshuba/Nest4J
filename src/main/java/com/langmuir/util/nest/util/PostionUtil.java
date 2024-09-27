package com.langmuir.util.nest.util;

import com.langmuir.util.nest.data.NestPath;
import java.util.List;

/**
 * Receive a set of NestPath, and transform its coordinates to the two-dimensional coordinate plane
 */
public class PostionUtil {

  public static List<NestPath> positionTranslate4Path(double x, double y, List<NestPath> paths) {
    for (NestPath path : paths) {
      path.translate(x, y);
      y = path.getMaxY() + 10;
    }
    return paths;
  }
}
