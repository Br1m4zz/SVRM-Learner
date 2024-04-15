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

package nl.cypherpunk.SVCSLearner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Configuration class used for learning parameters
 * 
 * @author Joeri de Ruiter (joeri@cs.ru.nl)
 */
public class LearningConfig {
	protected Properties properties;

	String output_dir = "output";
	String learning_algorithm = "lstar";
	String eqtest = "randomwords";
	
	// Used for W-Method and Wp-method
	int max_depth = 10;
	
	// Used for Random words
	int min_length = 5;
	int max_length = 10;
	int nr_queries = 100;
	int seed = 1;

	public LearningConfig(String filename) throws IOException {
		properties = new Properties();

		InputStream input = new FileInputStream(filename);
		properties.load(input);

		loadProperties();
	}
	
	public LearningConfig(LearningConfig config) {
		properties = config.getProperties();
		if(config.output_dir.length()>15)
			properties.setProperty("output_dir",config.output_dir);
		loadProperties();
	}
	
	public Properties getProperties() {
		return properties;
	}

	public void loadProperties() {
		if(properties.getProperty("output_dir").length() < 15)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
			output_dir = properties.getProperty("output_dir") + df.format(new Date());
		}
		else
		{
			output_dir = properties.getProperty("output_dir");
		}
		if(properties.getProperty("learning_algorithm").equalsIgnoreCase("lstar") || properties.getProperty("learning_algorithm").equalsIgnoreCase("dhc") || properties.getProperty("learning_algorithm").equalsIgnoreCase("kv") || properties.getProperty("learning_algorithm").equalsIgnoreCase("ttt") || properties.getProperty("learning_algorithm").equalsIgnoreCase("mp") || properties.getProperty("learning_algorithm").equalsIgnoreCase("rs")){
			learning_algorithm = properties.getProperty("learning_algorithm").toLowerCase();
		}
		else{
			System.out.println("[SVCS] Unknown Learning Alg, check properties.");
			System.exit(0);	
		}
			
		if(properties.getProperty("eqtest") != null && (properties.getProperty("eqtest").equalsIgnoreCase("wmethod") || properties.getProperty("eqtest").equalsIgnoreCase("modifiedwmethod") || properties.getProperty("eqtest").equalsIgnoreCase("wpmethod") || properties.getProperty("eqtest").equalsIgnoreCase("randomwords"))){
			eqtest = properties.getProperty("eqtest").toLowerCase();
		}
		else{
			System.out.println("[SVCS] Unknown EQ Alg, check properties.");
			System.exit(0);	
		}
			
		
		if(properties.getProperty("max_depth") != null)
			max_depth = Integer.parseInt(properties.getProperty("max_depth"));
		
		if(properties.getProperty("min_length") != null)
			min_length = Integer.parseInt(properties.getProperty("min_length"));
		
		if(properties.getProperty("max_length") != null)
			max_length = Integer.parseInt(properties.getProperty("max_length"));
		
		if(properties.getProperty("nr_queries") != null)
			nr_queries = Integer.parseInt(properties.getProperty("nr_queries"));
		if(properties.getProperty("seed") != null)
			seed = Integer.parseInt(properties.getProperty("seed"));

	}
}
