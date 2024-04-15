/*
 *  Copyright (c) 2016 Joeri de Ruiter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 */
package nl.cypherpunk.SVCSLearner;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.SUL;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

// Based on SULOracle from LearnLib by Falk Howar and Malte Isberner
@ParametersAreNonnullByDefault
public class LogOracle<I, D> implements MealyMembershipOracle<I,D> {
	public static class MealyLogOracle<I,O> extends LogOracle<I,O> {
		public MealyLogOracle(SUL<I, O> sul, LearnLogger logger) {
			super(sul, logger);
		}
	}

	LearnLogger logger;
	SUL<I, D> sul;

    public LogOracle(SUL<I,D> sul, LearnLogger logger) {
        this.sul = sul;
        this.logger = logger;
    }

    @Override
	public Word<D> answerQuery(Word<I> prefix, Word<I> suffix) {
		WordBuilder<D> wbPrefix = new WordBuilder<>(prefix.length());
		WordBuilder<D> wbSuffix = new WordBuilder<>(suffix.length());
		this.sul.pre();
		try {
			// Prefix: Execute symbols, only log output		
			for(I sym : prefix) {
				wbPrefix.add(this.sul.step(sym));
			}

			// Suffix: Execute symbols, outputs constitute output word		
			for(I sym : suffix) {
				wbSuffix.add(this.sul.step(sym));
			}

	    	System.out.println("\033[36;4m"+"[" + prefix.toString() + " | " + suffix.toString() + "\033[32;4m"+ " ## " +"\033[0m"+ wbPrefix.toWord().toString() + " | " + wbSuffix.toWord().toString() + "]" );
			
		}
		finally {
			sul.post();
		}

		return wbSuffix.toWord();
    }

	@Override
    @SuppressWarnings("unchecked")
	public Word<D> answerQuery(Word<I> query) {
		return answerQuery((Word<I>)Word.epsilon(), query);
    }

    @Override
    public MembershipOracle<I, Word<D>> asOracle() {
    	return this;
    }

	@Override
	public void processQueries(Collection<? extends Query<I, Word<D>>> queries) {
		for (Query<I,Word<D>> q : queries) {
			Word<D> output = answerQuery(q.getPrefix(), q.getSuffix());
			q.answer(output);
		}
	}
}
