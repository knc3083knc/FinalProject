package com.example.aneazxo.finalproject.find_path.graph;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Wuttinan
 */
public class Node {
    private String ID;
    private double g = 0;//Double.POSITIVE_INFINITY;
    private double h = Double.POSITIVE_INFINITY;
    private double f = Double.POSITIVE_INFINITY;
    private Node previous;
    private double lat,lng;
    private boolean visited = false;
    //private boolean selected = false;
    private boolean isBarrier = false;
    
    public Node(){
        
    }
    
    public Node(String id, double lat, double lng){
        this.ID = id;
        this.lat = lat;
        this.lng = lng;
    }

    public double getG() {
        return g;
    }

    public void setG(double g) {
        this.g = g;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public Node getPrevious() {
        return previous;
    }

    public void setPrevious(Node previous) {
        this.previous = previous;
    }

    public double getLat() {
        return lat;
    }

    public void setRow(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
    public boolean isIsBarrier() {
        return isBarrier;
    }

    public void setIsBarrier(boolean isBarrier) {
        this.isBarrier = isBarrier;
    }

    public String getID() {
        return ID;
    }
    
    public boolean isSameNode(Node n){
        return this.ID.equals(n.getID());
    }
    
    public String toString(){
        return this.getID();/*previous + " " + lat +"," +lng;*/
    }
    
/*
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
*/
}
