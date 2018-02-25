package com.upseil.game.component;

import com.artemis.Component;

public class GameState extends Component {
    
    private int score;

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        incrementScore(1);
    }
    
    public void incrementScore(int amount) {
        score += amount;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
}
