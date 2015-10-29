package com.watch.iot.iotwatchface2;

/**
 * Created by who on 2015-10-29.
 */
public class Gadget {
    public int type,id;
    private double xPosInRoom,yPosInRoom;
    public double angle;

    public Gadget(int type, int id, double xPosInRoom, double yPosInRoom) {
        this.type=type;
        this.id=id;
        this.xPosInRoom=xPosInRoom;
        this.yPosInRoom=yPosInRoom;
        this.angle=Math.atan(yPosInRoom/xPosInRoom);
    }


}
