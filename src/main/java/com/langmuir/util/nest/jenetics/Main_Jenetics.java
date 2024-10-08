package com.langmuir.util.nest.jenetics;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import static io.jenetics.engine.Limits.bySteadyFitness;

import com.langmuir.util.nest.Nest;
import com.langmuir.util.nest.config.Config;
import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.data.Placement;
import com.langmuir.util.nest.data.Segment;
import com.langmuir.util.nest.gui.GuiUtil;
import com.langmuir.util.nest.util.CommonUtil;
import com.langmuir.util.nest.util.GeometryUtil;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.MeanAlterer;
import io.jenetics.MultiPointCrossover;
import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.SwapMutator;
import io.jenetics.UniformCrossover;
import io.jenetics.engine.Constraint;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import io.jenetics.util.Factory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.dom4j.DocumentException;


public class Main_Jenetics {

  public static Phenotype<DoubleGene, Double> tmpBest = null;

  public static void main(String[] args) {
    final int MAX_SEC_DURATION = 60;

    NestPath bin = new NestPath();
    double binWidth = 400;
    double binHeight = 400;

    bin.add(0, 0);
    bin.add(binWidth, 0);
    bin.add(binWidth, binHeight);
    bin.add(0, binHeight);

    List<NestPath> polygons = null;

    try {
      polygons = GuiUtil.transferSvgIntoPolygons(null);
    } catch (DocumentException e) {
      e.printStackTrace();
    }

    Config config = new Config();
    config.SPACING = 0;
    config.POPULATION_SIZE = polygons.size();
    Config.BIN_HEIGHT = binHeight;
    Config.BIN_WIDTH = binWidth;
    config.N_THREAD = 5;

    List<NestPath> tree = CommonUtil.BuildTree(polygons, Config.CURVE_TOLERANCE);
    CommonUtil.offsetTree(tree, 0.5 * config.SPACING);

    bin.config = config;
    for (NestPath nestPath : polygons) {
      nestPath.config = config;
    }

    NestPath binPolygon = CommonUtil.cleanNestPath(bin);  //conversione di un eventuale binPath self intersecting in un poligono semplice

    // //The max and min coordinates of the single bin path are set in order to be able to translate it to the origin
    //    double xbinmax = binPolygon.get(0).x;	// get.(0) = prende il primo segmento dei 4 (coordinate del primo vertice), se si assume che la superficie sia rettangolare
    //    double xbinmin = binPolygon.get(0).x;
    //    double ybinmax = binPolygon.get(0).y;
    //    double ybinmin = binPolygon.get(0).y;
    //    // Find min max
    //    for(int i = 1 ; i<binPolygon.size(); i++){
    //        if(binPolygon.get(i).x > xbinmax ){
    //            xbinmax = binPolygon.get(i).x;
    //        }
    //        else if (binPolygon.get(i).x < xbinmin ){
    //            xbinmin = binPolygon.get(i) .x;
    //        }
    //
    //        if(binPolygon.get(i).y > ybinmax ){
    //            ybinmax = binPolygon.get(i).y;
    //        }
    //        else if (binPolygon.get(i). y <ybinmin ){
    //            ybinmin = binPolygon.get(i).y;
    //        }
    //    }
    //
    //    /*VIENE TRASLATO IL POLIGONO BINPATH NELL'ORIGINE*/
    //    for(int i=0; i<binPolygon.size(); i++){
    //        binPolygon.get(i).x -= xbinmin;
    //        binPolygon.get(i).y -= ybinmin;
    //    }
    //
    //    if(GeometryUtil.polygonArea(binPolygon) > 0 ){
    //        binPolygon.reverse();
    //    }

    if (Config.BOUND_SPACING > 0) {
      List<NestPath> offsetBin = CommonUtil.polygonOffset(binPolygon, -Config.BOUND_SPACING);
      if (offsetBin.size() == 1) {
        binPolygon = offsetBin.getFirst();
      }
    }
    binPolygon.setId(-1);
    // A part may become not positionable after a rotation. TODO this can also be removed if we know that all parts are legal
    if (!Config.ASSUME_ALL_PARTS_PLACABLE) {
      List<Integer> integers = Nest.checkIfCanBePlaced(binPolygon, tree);
      List<NestPath> safeTree = new ArrayList<>();
      for (Integer i : integers) {
        safeTree.add(tree.get(i));
      }
      tree = safeTree;
    }

    for (NestPath element : tree) {
      Segment start = element.getSegment(0);
      Segment end = element.getSegment(element.size() - 1);
      if (start == end || GeometryUtil.almostEqual(start.x, end.x) && GeometryUtil.almostEqual(start.y, end.y)) {
        element.removeLastSegment();
      }
      if (GeometryUtil.polygonArea(element) > 0) {
        element.reverse();
      }
    }

    for (NestPath np : tree) {
      np.Zerolize();
      np.translate((binWidth - np.getMaxX()) / 2, (binHeight - np.getMaxY()) / 2);
      //np.setPossibleNumberRotations(4);
    }

    //ExecutorService executor = Executors.newFixedThreadPool(10);
    FitnessModel fm = new FitnessModel(tree, binWidth, binHeight);
    Factory<Genotype<DoubleGene>> model = ModelFactory.of(tree, binWidth, binHeight);
    final Constraint<DoubleGene, Double> constraint = new CustomConstraint(tree);
    final EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

    ExecutorService executor = Executors.newFixedThreadPool(config.N_THREAD);

    Engine<DoubleGene, Double> engine = Engine.builder(fm::scalarFitness, model)
        .populationSize(config.POPULATION_SIZE)
        .optimize(Optimize.MINIMUM)
        .executor(executor)
        .alterers(
            new Mutator<>(.25),
            new MeanAlterer<>(.05),
            new SwapMutator<>(0.25),
            new UniformCrossover<>(0.05),
            new MultiPointCrossover<>(0.05)
        )
        // .executor(executor)
        .constraint(constraint)
        .build();


    System.out.println("Starting nesting of " + polygons.size() + " polygons in " + binWidth + " * " + binHeight + " bin");
    System.out.println("population size: " + config.POPULATION_SIZE + " - Max duration: " + MAX_SEC_DURATION + "s");

    final Phenotype<DoubleGene, Double> best = engine.stream()
        .limit(bySteadyFitness(polygons.size() * 10))
        .limit(Limits.byExecutionTime(Duration.ofSeconds(MAX_SEC_DURATION)))
        .peek(Main_Jenetics::update)
        .peek(statistics)
        .collect(toBestPhenotype());


    // ArrayList<NestPath> polys = CommonUtil.cloneArrayListNestpath(tree);
    List<List<Placement>> appliedplacement = ModelFactory.convert(best.genotype(), tree);

    //compress(tree);


    try {
      List<String> res = createSvg(tree, binWidth, binHeight);
      GuiUtil.saveSvgFile(res, Config.OUTPUT_DIR + "res.html", binWidth, binHeight);

    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println(statistics);
    System.out.println(best);


  }

  //	private static void compress(List<NestPath> list)
  //	{
  //		 for(int i=0; i<list.size();i++)
  //	        {
  //	        	NestPath pi = list.get(i);
  //	        	pi.ZeroX();
  //	        	for(int j=0;j<list.size();j++)
  //	        	{
  //	        		NestPath pj = list.get(j);
  //	        		if(i!=j)
  //	        		{
  //	        			while(GeometryUtil.intersect(pi, pj))
  //	        				{pi.translate(1, 0);}
  //	        		}
  //	        	}
  //	        }
  //	        for(int i=0; i<list.size();i++)
  //	        {
  //	        	NestPath pi = list.get(i);
  //	        	pi.ZeroY();
  //	        	for(int j=0;j<list.size();j++)
  //	        	{
  //	        		NestPath pj = list.get(j);
  //	        		if(i!=j)
  //	        		{
  //	        			while(GeometryUtil.intersect(pi, pj))
  //	        				{pi.translate(0, 1);}
  //	        		}
  //	        	}
  //	        }
  //	}

  /**
   * Function to be executed at every generation
   * If the result is the best until now show a message
   *
   * @param result result of evaluation
   */
  private static void update(final EvolutionResult<DoubleGene, Double> result) {
    if (tmpBest == null || tmpBest.compareTo(result.bestPhenotype()) > 0) {
      tmpBest = result.bestPhenotype();
      System.out.println(result.generation() + " generation: ");
      System.out.println("Found better fitness: " + tmpBest.fitness());
    }
  }

  /**
   * @param list      List of NestPaths nested
   * @param binwidth  Width of bin
   * @param binheight height of bin
   * @return
   */
  public static List<String> createSvg(List<NestPath> list, double binwidth, double binheight) {

    List<String> strings = new ArrayList<>();
    StringBuilder s = new StringBuilder("    <rect x=\"0\" y=\"0\" width=\"" + binwidth + "\" height=\"" + binheight + "\"  fill=\"none\" stroke=\"#010101\" stroke-width=\"1\" />\n");

    for (NestPath nestPath : list) {
      //        	double ox = placement.translate.x;
//        	double oy = placement.translate.y;
//        	double rotate = placement.rotate;
      //s += "<g transform=\"translate(" + ox + x + " " + oy + y + ") rotate(" + rotate + ")\"> \n";
      s.append("<path id=\"").append(nestPath.getBid()).append("\" d=\"");
      for (int i = 0; i < nestPath.getSegments().size(); i++) {
        if (i == 0) {
          s.append("M");
        } else {
          s.append("L");
        }
        Segment segment = nestPath.getSegment(i);
        s.append(segment.x).append(" ").append(segment.y).append(" ");
      }
      s.append("Z\" fill=\"#8498d1\" stroke=\"#010101\" stroke-width=\0.5\" />" + " \n");
      //s += "</g> \n";
    }
    //y += binHeight + 50;
    strings.add(s.toString());

    return strings;


  }

}
