package com.example.aneazxo.finalproject.find_path;

import com.example.aneazxo.finalproject.find_path.graph.Edge;
import com.example.aneazxo.finalproject.find_path.graph.Graph;

import java.util.ArrayList;

/**
 * Created by AneazXo on 04-Feb-17.
 */

public interface ShortestPath {

    ArrayList<Integer> findShortestPath(int source, int target, Graph<? extends Edge> g);

}
