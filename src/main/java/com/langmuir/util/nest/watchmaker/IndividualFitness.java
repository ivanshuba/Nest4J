package com.langmuir.util.nest.watchmaker;

import com.langmuir.util.nest.algorithm.Individual;
import com.langmuir.util.nest.config.Config;
import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.data.Result;
import com.langmuir.util.nest.util.Placementworker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

public class IndividualFitness implements FitnessEvaluator<Individual> {


  @Override
  public double getFitness(Individual individuo, List<? extends Individual> listIndiv) {
    /* Valore standard della fitness (appena si crea un Individuo (Individual) = -1 ; nel nesting il BestFitness sarà quello con la fitness minore (Nest.launchWorkers()) */

    // Creazione Mappa (Map(chiave, valore))
    Map<String, List<NestPath>> nfpCache = new HashMap<>();

    // Creazione Placementworker che restituirà la fitness dell'Individuo
    Placementworker placePoly = new Placementworker(new CandidateFactoryNest4j().getRandomNestPath(9000), new Config(), nfpCache);
    Result result = placePoly.placePaths(individuo.getPlacement());
    System.out.println("Area = " + result.area + "	, 	Fitness = " + result.fitness);

    return result.fitness;
  }

  @Override
  public boolean isNatural() {
    return true;
  }
}