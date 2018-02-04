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
import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

    @Override
    public int compare(Node o1, Node o2) {
        if (o1.getF() < o2.getF()) {
            return -1;
        }
        if (o1.getF() > o2.getF()) {
            return 1;
        }
        return 0;
    }
}
