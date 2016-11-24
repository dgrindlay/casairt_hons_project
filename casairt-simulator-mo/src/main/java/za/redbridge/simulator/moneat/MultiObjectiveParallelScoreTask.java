/*
 * Encog(tm) Core v3.3 - Java Version
 * http://www.heatonresearch.com/encog/
 * https://github.com/encog/encog-java-core

 * Copyright 2008-2014 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information on Heaton Research copyrights, licenses
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package za.redbridge.simulator.moneat;

import java.util.List;
import java.util.ArrayList;

import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;
import org.encog.ml.ea.exception.EARuntimeError;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.score.AdjustScore;
import org.encog.ml.ea.train.basic.BasicEA;
import za.redbridge.simulator.ScoreCalculator;
import za.redbridge.simulator.moneat.MultiObjectiveScore;

/**
 * An individual threadable task for the parallel score calculation.
 *
 */
public class MultiObjectiveParallelScoreTask implements Runnable {

	/**
	 * The genome to calculate the score for.
	 */
	private final Genome genome;

	/**
	 * The score function.
	 */
	private final CalculateScore scoreFunction;

	/**
	 * The score adjusters.
	 */
	private final List<AdjustScore> adjusters;

	/**
	 * The owners.
	 */
	private final MultiObjectiveParallelScore owner;

	/**
	 * Construct the parallel task.
	 * @param genome The genome.
	 * @param theOwner The owner.
	 */
	public MultiObjectiveParallelScoreTask(Genome genome, MultiObjectiveParallelScore theOwner) {
		super();
		this.owner = theOwner;
		this.genome = genome;
		this.scoreFunction = theOwner.getScoreFunction();
		this.adjusters = theOwner.getAdjusters();
	}

	/**
	 * Perform the task.
	 */
	@Override
	public void run() {
		MLMethod phenotype = this.owner.getCodec().decode(this.genome);
		if (phenotype != null) {
			ArrayList<Double> scoreValues = new ArrayList<Double>();
			try {
				scoreValues = ((ScoreCalculator)this.scoreFunction).calculateMultipleScores(phenotype);
			} catch(EARuntimeError e) {

			}

            // create MultiObjectiveScore object and add values
			MultiObjectiveScore genomeScore = new MultiObjectiveScore(owner.getPopulation().getPopulationObjectives());
			genomeScore.setObjectiveScores(scoreValues);
			owner.getPopulationMap().put(this.genome, genomeScore);
		} else {

		}
	}
}
