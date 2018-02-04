package com.example.aneazxo.finalproject.find_path.algs;


import android.util.Log;

import com.example.aneazxo.finalproject.core.Tool;
import com.example.aneazxo.finalproject.find_path.graph.Node;
import com.example.aneazxo.finalproject.find_path.graph.NodeComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Wuttinan
 */
public class AStar {

    private ArrayList<String> adjList = new ArrayList<>();
    private ArrayList<Node> nodeList = new ArrayList<>();
    private ArrayList<Node> pathList = new ArrayList<>();

    /*
    public void readFile(String filePath) {

        File f = new File(filePath);

        try {

            //FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tmp = line.split(",");
                //System.out.println(line);
                String ID = tmp[0];
                //String name = tmp[1];
                String lat = tmp[2];
                String lng = tmp[3];
                String adj = tmp[4];
                Node n = new Node(ID, Double.parseDouble(lat), Double.parseDouble(lng));
                nodeList.add(n);
                adjList.add(adj);
            }

        } catch (IOException ex) {
            Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
    public void prepare (ArrayList<String> info) {
        for (int i = 0; i < info.size(); i++) {
            String[] tmp = info.get(i).split(",");
            //System.out.println(line);
            String ID = tmp[0];
            //String name = tmp[1];
            String lat = tmp[2];
            String lng = tmp[3];
            String adj = tmp[4];
            Node n = new Node(ID, Double.parseDouble(lat), Double.parseDouble(lng));
            nodeList.add(n);
            adjList.add(adj);
        }
    }

    public ArrayList<Integer> findPath(int oriID, int desID) {
        ArrayList<Integer> ans = new ArrayList<Integer>();
        Comparator<Node> comparator = new NodeComparator();
        PriorityQueue<Node> queue = new PriorityQueue<>(10, comparator);

        Node ori = nodeList.get(oriID);
        Node des = nodeList.get(desID);

        boolean pathFound = false;
        queue.add(ori);
        ori.setVisited(true);

        while (!queue.isEmpty()) {
            Node curr = queue.remove();
            ans.add(Integer.parseInt(curr.getID()));
            if (curr.isSameNode(des)) {
                pathFound = true;
                break;
            }

            String[] adjID = adjList.get(Integer.parseInt(curr.getID())).split("-");
            for (int i = 0; i < adjID.length; i++) {
                if (Integer.parseInt(adjID[i]) < nodeList.size()) {
                    Node adj = nodeList.get(Integer.parseInt(adjID[i]));
                    if (!adj.isVisited()) {
                        adj.setH(distFrom(adj, des));
                        adj.setF(adj.getG() + adj.getH());
                        adj.setVisited(true);
                        queue.add(adj);
                    }
                } else {
                    Log.e("AStar", "findPath: index of of length (" + Integer.parseInt(adjID[i]) + "/" + adjID.length);
                    Tool.cleanDatabaseFile();
                }
            }
        }

        if (pathFound) {
            /*
            for (Integer n : ans) {
                System.out.print(n + " ");
            }
            System.out.println("");
            */
        } else {
            //System.out.println("Path Not Found");
            return new ArrayList<Integer>();
        }

        return ans;
    }

    private double distFrom(Node ori, Node des) {
        double lat1 = ori.getLat();
        double lng1 = ori.getLng();

        double lat2 = des.getLat();
        double lng2 = des.getLng();

        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

}
