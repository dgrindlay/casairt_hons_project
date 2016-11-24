package za.redbridge.simulator;

import java.util.HashMap;
import java.util.Map;

import za.redbridge.simulator.phenotype.Phenotype;

/**
 *  Class that keeps track of a phenotypes fitness - unused class
 */
public class FitnessStats {
    private final Map<Phenotype,Double> phenotypeFitnesses = new HashMap<>();
    private double teamFitness = 0.0;
    private int maxSteps;

    public FitnessStats(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /**
     * Increment a phenotype's fitness.
     * @param phenotype the phenotype who's score will be adjusted
     * @param adjustedValue the adjusted value of the resource
     */
    public void addToPhenotypeFitness(Phenotype phenotype, double fitnessValue) {
        phenotypeFitnesses.put(phenotype, getPhenotypeFitness(phenotype) + fitnessValue);
    }

    public double getPhenotypeFitness(Phenotype phenotype) {
        return phenotypeFitnesses.getOrDefault(phenotype, 0.0);
    }

    /**
     * Increment the team fitness.
     * @param value the unadjusted resource value
     */
    public void addToTeamFitness(double value)
    {
        teamFitness += value;
    }

    /** Gets the normalized team fitness including time bonus (out of 120) */
    public double getTeamFitnessWithTimeBonus(long stepsTaken) {
        return teamFitness + (stepsTaken / maxSteps) * 20;
    }

    /** Gets the team fitness value */
    public double getTeamFitness() {
        return teamFitness;
    }

    public Map<Phenotype,Double> getPhenotypeFitnessMap() {
        return phenotypeFitnesses;
    }
}
