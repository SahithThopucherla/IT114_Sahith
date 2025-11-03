// st944 10-30-25
//Payload class in charge of sending player point updates
package Project.Common;

import Project.Common.Payload;

public class PointsPayload extends Payload {
    private int points;     //stores players score
    public int getPoints() {    // gets user score
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    @Override
    public String toString() {
        return super.toString() + String.format(", Points: [%d]", getPoints());
    }
}