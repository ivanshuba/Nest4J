package com.langmuir.util.nest.watchmaker;

import com.langmuir.util.nest.algorithm.GeneticAlgorithm;
import com.langmuir.util.nest.algorithm.Individual;
import com.langmuir.util.nest.config.Config;
import com.langmuir.util.nest.data.NestPath;
import com.langmuir.util.nest.util.CommonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

// fa ruotazioni a caso
public class IndividualMutation implements EvolutionaryOperator<Individual> {

  @Override
  public List<Individual> apply(List<Individual> indivList, Random random) {

    Config config = new Config();
    NestPath bin = new CandidateFactoryNest4j().getRandomNestPath(1);  // generazione random di un NestPath
//		System.out.println("BIN = " + bin.toString());


    List<NestPath> tree = CommonUtil.BuildTree(indivList.get(0).getPlacement(), Config.CURVE_TOLERANCE);
    CommonUtil.offsetTree(tree, 0.5 * config.SPACING);

    List<NestPath> adam = new ArrayList<>();
    for (NestPath np : tree) {
      adam.add(np);
    }


    // MUTATION
    GeneticAlgorithm ga = new GeneticAlgorithm(adam, bin, new Config());  // Creazione di 10 individui mutando il primo individuo (adam)


    // CROSSOVER
    ga.generation();  // generazione dei figli - PERò MI PRENDE SEMPRE E SOLO IL PRIMO, perchè, dato che vengono ordinati (sort) verrrà sempre selezionato l'individuo avente AREA MINORE, quindi il "the best fit"


    return ga.population;  // verranno restituiti sempre 10 individui (POPULATION_SIZE = 10 by default)
  }

}
