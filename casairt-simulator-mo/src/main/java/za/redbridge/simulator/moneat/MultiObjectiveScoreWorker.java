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

import java.util.Random;
import java.util.concurrent.Callable;

import org.encog.EncogError;
import org.encog.ml.ea.exception.EARuntimeError;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.opp.EvolutionaryOperator;
import org.encog.ml.ea.species.Species;
import org.encog.ml.ea.train.basic.*;

/**
 * A worker thread for an Evolutionary Algorithm.
 */
public class MultiObjectiveScoreWorker implements Callable<Object> {

	/**
	 * The genome being evaluated.
	 */
	private final MultiObjectiveGenome genome;

	/**
	 * The parent object.
	 */
	private final MultiObjectiveBasicEA train;

	/**
	 * Construct the EA worker.
	 *
	 * @param theTrain
	 *            The trainer.
	 * @param theSpecies
	 *            The species.
	 */
	public MultiObjectiveScoreWorker(final MultiObjectiveBasicEA theTrain, final MultiObjectiveGenome theGenome) {
		this.train = theTrain;
        this.genome = theGenome;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object call() {
        this.train.calculateScore(this.genome);
        return null;
	}
}
