package com.langmuir.util.nest.util;

import com.langmuir.util.nest.config.Config;
import com.langmuir.util.nest.data.Bound;
import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.data.NfpPair;
import com.langmuir.util.nest.data.ParallelData;
import java.util.List;

/**
 * @author yisa
 */
public class NfpUtil {

  /**
   * Get a pair of polygons and generate nfp
   *
   * @param pair
   * @param config
   * @return
   */
  public static ParallelData nfpGenerator(NfpPair pair, Config config) {
    boolean searchEdges = config.isCONCAVE();
    boolean useHoles = config.isUSE_HOLE();

    NestPath A = GeometryUtil.rotatePolygon2Polygon(pair.getA(), pair.getKey().getArotation());
    NestPath B = GeometryUtil.rotatePolygon2Polygon(pair.getB(), pair.getKey().getBrotation());

    List<NestPath> nfp;
    if (pair.getKey().isInside()) {
      if (GeometryUtil.isRectangle(A, 0.001)) {
        nfp = GeometryUtil.noFitPolygonRectangle(A, B);
      } else {
        nfp = GeometryUtil.noFitPolygon(A, B, true, searchEdges);
      }
      if (nfp != null && !nfp.isEmpty()) {
        for (NestPath element : nfp) {
          if (GeometryUtil.polygonArea(element) > 0) {
            element.reverse();
          }
        }
      }
    } else {
      int count = 0;
      if (searchEdges) {
        // NFP Generator TODO  double scale control
        nfp = GeometryUtil.noFitPolygon(A, B, false, searchEdges);
      } else {
        nfp = GeometryUtil.minkowskiDifference(A, B);
      }
      if (nfp.isEmpty()) {
        return null;
      }

      //TODO This test is causing null result
      for (int i = 0; i < nfp.size(); i++) {
        if (!searchEdges || i == 0) {
          // TODO why here: normally the area of nfp should be greater than A
          if (Math.abs(GeometryUtil.polygonArea(nfp.get(i))) < Math.abs(GeometryUtil.polygonArea(A))) {
            nfp.remove(i);
            return null;
          }
        }
      }

      if (nfp.isEmpty()) {
        return null;
      }

      for (int i = 0; i < nfp.size(); i++) {
        if (GeometryUtil.polygonArea(nfp.get(i)) > 0) {
          nfp.get(i).reverse();
        }

        if (i > 0) {
          if (Boolean.TRUE.equals(GeometryUtil.pointInPolygon(nfp.get(i).getSegment(0), nfp.getFirst()))) {
            if (GeometryUtil.polygonArea(nfp.get(i)) < 0) {
              nfp.get(i).reverse();
            }
          }
        }
      }

      if (useHoles && !A.getChildren().isEmpty()) {
        Bound Bbounds = GeometryUtil.getPolygonBounds(B);
        for (int i = 0; i < A.getChildren().size(); i++) {
          Bound Abounds = GeometryUtil.getPolygonBounds(A.getChildren().get(i));
          if (Abounds.width > Bbounds.width && Abounds.height > Bbounds.height) {
            List<NestPath> cnfp = GeometryUtil.noFitPolygon(A.getChildren().get(i), B, true, searchEdges);
            if (!cnfp.isEmpty()) {
              for (NestPath nestPath : cnfp) {
                if (GeometryUtil.polygonArea(nestPath) < 0) {
                  nestPath.reverse();
                }
                nfp.add(nestPath);
              }
            }
          }
        }
      }
    }
    return new ParallelData(pair.getKey(), nfp);
  }
}
