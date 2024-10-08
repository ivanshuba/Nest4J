package com.langmuir.util.nest.util;

import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.data.Placement;
import com.langmuir.util.nest.data.Segment;
import java.util.ArrayList;
import java.util.List;

public class SvgUtil {

  /**
   * The polygons are colored in light red if they are being rotated, in light blue elsewhere
   *
   * @param list      List<NestPath> to be converted in SVG
   * @param applied   List<List<Placement>> corresponding to the placements of the NestPaths on the bins
   * @param binWidth  width of the bins
   * @param binHeight height of the bins
   * @throws Exception
   * @return List<String> corresponding to the SVG elements
   */
  public static List<String> svgGenerator(List<NestPath> list,
                                          List<List<Placement>> applied,
                                          double binWidth,
                                          double binHeight) throws Exception {
    List<String> strings = new ArrayList<>();
    int x = 0;
    int y = 0;
    for (List<Placement> binlist : applied) {
      StringBuilder s = new StringBuilder();
      s.append(String.format("""
           <g transform="translate(%d %d)">
          """, x, y));
      s.append(String.format("""
          <rect x="0" y="0" width="%f" height="%f" fill="none" stroke="#010101" stroke-width="1" />
          """, binWidth, binHeight));
      for (Placement placement : binlist) {
        int bid = placement.bid;
        NestPath nestPath = getNestPathByBid(bid, list);
        assert nestPath != null;
        int numSegments = nestPath.getSegments().size();
        double ox = placement.translate.x;
        double oy = placement.translate.y;
        double rotate = placement.rotate;
        s.append(String.format("""
            <g transform="translate(%f %f) rotate(%f)">
            """, ox + x, oy + y, rotate));
        s.append("<path d=\"");
        for (int i = 0; i < numSegments; i++) {
          s.append(i == 0 ? "M" : "L");
          Segment segment = nestPath.getSegment(i);
          s.append(segment.x).append(" ").append(segment.y).append(" ");
        }
        s.append("Z\" fill=\"none\" stroke=\"#010101\" stroke-width=\"0.5\" />" + " \n");
        s.append("</g> \n");
      }
      s.append("</g> \n");
      y += (int) (binHeight + 50);
      strings.add(s.toString());
    }
    return strings;
  }

  private static NestPath getNestPathByBid(int bid, List<NestPath> list) {
    for (NestPath nestPath : list) {
      if (nestPath.getBid() == bid) {
        return nestPath;
      }
    }
    return null;
  }

}
