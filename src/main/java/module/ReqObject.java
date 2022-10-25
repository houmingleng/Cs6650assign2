package module;

import io.swagger.client.model.LiftRide;

public class ReqObject {
    private String skierID;
    private String resortID;
    LiftRide curLiftRide;
    private int liftID;
    private String dayID;
    private String seasonID;
    private int time;
    public ReqObject(String skierID, String resortID, int liftID, String dayID, String seasonID, int time, LiftRide curLiftRide) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.dayID = dayID;
        this.seasonID = seasonID;
        this.time = time;
        this.curLiftRide = curLiftRide;
    }

    public LiftRide getCurLiftRide() {
        return curLiftRide;
    }

    public void setCurLiftRide(LiftRide curLiftRide) {
        this.curLiftRide = curLiftRide;
    }

    public String getSkierID() {
        return skierID;
    }

    public void setSkierID(String skierID) {
        this.skierID = skierID;
    }

    public String getResortID() {
        return resortID;
    }

    public void setResortID(String resortID) {
        this.resortID = resortID;
    }

    public int getLiftID() {
        return liftID;
    }

    public void setLiftID(int liftID) {
        this.liftID = liftID;
    }

    public String getDayID() {
        return dayID;
    }

    public void setDayID(String dayID) {
        this.dayID = dayID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "reqObject{" +
                "skierID=" + skierID +
                ", resortID=" + resortID +
                ", liftID=" + liftID +
                ", dayID='" + dayID + '\'' +
                ", seasonID='" + seasonID + '\'' +
                ", time=" + time +
                '}';
    }
}
