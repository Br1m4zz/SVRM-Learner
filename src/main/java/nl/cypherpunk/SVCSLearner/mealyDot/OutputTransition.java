package nl.cypherpunk.SVCSLearner.mealyDot;

import java.io.Serializable;

public class OutputTransition implements Serializable {
    public String output;
    public MealyState state;

    public OutputTransition(String output, MealyState state){
        this.output = output;
        this.state = state;
    }
}
