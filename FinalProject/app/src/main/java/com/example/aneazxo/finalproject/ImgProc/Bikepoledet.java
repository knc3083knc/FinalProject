/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.aneazxo.finalproject.ImgProc;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Wuttinan
 */
public class Bikepoledet extends AppCompatActivity {

    private static String TAG = "MainActivityOld";

    public static String bikepoleDetect(Mat mRGB, Point vp, double height, double pix_size, double down_samp, double foc) {

        Mat bikep = new Mat();
        Core.inRange(mRGB, new Scalar(0, 0, 105), new Scalar(50, 50, 255), bikep);        // Red => r[105, 255], g[0, 50], b[0, 50]

        Mat kernal = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(bikep, bikep, Imgproc.MORPH_CLOSE, kernal);
        Imgproc.morphologyEx(bikep, bikep, Imgproc.MORPH_OPEN, kernal);
        kernal.release();

        List<MatOfPoint> contours = new ArrayList(); //[0] => x = vp(2), [1] => y = vp(1)
        Imgproc.findContours(bikep, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        // 1. not find angle
        double minX = 0;
        double maxX = 0;
        double maxY = 0;
        String ans = "none";
        
        if (contours.size() > 6) {
            for (MatOfPoint tmp : contours) { // each red band
                Mat tmpRow = new Mat(tmp.rows(), 1, CvType.CV_32F);
                Mat tmpCol = new Mat(tmp.rows(), 1, CvType.CV_32F);
                for (int j = 0; j < tmp.rows(); j++) {
                    double[] a = tmp.get(j, 0);
                    tmpRow.put(j, 0, (int) a[1]);
                    tmpCol.put(j, 0, (int) a[0]);
                }

                double y = Core.minMaxLoc(tmpRow).maxVal;
                maxY = Math.max(maxY, y);

                double x1 = Core.minMaxLoc(tmpCol).minVal;
                minX = Math.min(minX, x1);
                double x2 = Core.minMaxLoc(tmpCol).maxVal;
                maxX = Math.max(maxX, x2);
                tmp.release();
            }

            double meanX = ((minX + maxX) / 2);
            double Z = (height * foc) / ((maxY - vp.y) * pix_size * down_samp);
            double X = (height * Math.abs(vp.x - meanX)) / ((maxY - vp.y));

            double dis = Math.sqrt(Math.pow(Z, 2) + Math.pow(X, 2));

            Log.d(TAG, "Bike poledet Detected: dist = " + dis);
            ans = "Bike poledet Detected: dist = " + dis;
             //************** debug **********/
            Imgproc.circle(mRGB, new Point(meanX, maxY), 5, new Scalar(0, 255, 0), 2);
            
            /********************************
            Imgcodecs.imwrite("C:\\Users\\Wuttinan\\Desktop\\Project\\Result\\bikepole_" + filename, mRGB);            
             /********************************/

            /*/************** 2. find angle too   
             /********************************/
        }
        return ans;
    }

}
