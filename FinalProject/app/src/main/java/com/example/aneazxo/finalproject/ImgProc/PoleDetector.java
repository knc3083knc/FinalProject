package com.example.aneazxo.finalproject.ImgProc;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class PoleDetector {

    public PoleDetector() {
    }

    /**
     * Find possible pole location from given gray image.
     *
     * @param imGray       source gray image.
     * @param vp           vanishing point.
     * @param height       vertical height of camera from ground.
     * @param pixelSize    pixel size.
     * @param downSampling down sampling ratio.
     * @param focal        camera's focal length.
     * @return ArrayList of results:
     * <ul>
     * <li>[0] - (polePoints Point[]) pixel coordinates (x,y) of found poles.</li>
     * <li>[1] - (angles double[]) angles of each pole with respect to vanishing line. Negative angle - pole is on left side. Positive angle - pole is on right side</li>
     * <li>[2] - (distances double[]) distant from camera to each pole.</li>
     * </ul>
     */
    public static ArrayList<Object> detect(Mat imGray, Point vp, double height,
                                           double pixelSize, byte downSampling, double focal) {

        ArrayList<Object> result = null;

        int rows = imGray.rows();
        int cols = imGray.cols();
/*
// START OLD ====================================
		// matA is left half below vanishing point
		// matB is right half below vanishing point
		Mat matA = imGray.submat((int) (vp.y + 1), rows, 0, (int) (vp.x + 1));
		Mat matB = imGray.submat((int) (vp.y + 1), rows, (int) (vp.x + 1), cols);

		double t1 = Core.mean(matA).val[0];
		double t2 = Core.mean(matB).val[0];

		Mat matA1 = new Mat();
		Mat matB1 = new Mat();

		Mat cond1 = new Mat();
		Mat cond2 = new Mat();

		// A1 = A>1.5*t1 & A > 100;
		Core.compare(matA, new Scalar(1.5 * t1), cond1, Core.CMP_GT);
		Core.compare(matA, new Scalar(100), cond2, Core.CMP_GT);
		Core.bitwise_and(cond1, cond2, matA1);

		Core.compare(matB, new Scalar(1.5 * t2), cond1, Core.CMP_GT);
		Core.compare(matB, new Scalar(100), cond2, Core.CMP_GT);
		Core.bitwise_and(cond1, cond2, matB1);

		// bw = [A1 B1];
		List<Mat> listMat = new ArrayList<>();
		listMat.add(matA1);
		listMat.add(matB1);
		Mat bw = new Mat();
		Core.hconcat(listMat, bw);
// END OLD ======================================
*/

// START NEW ====================================
        Mat bw = new Mat();
        Mat cond1 = new Mat();
        Mat cond2 = new Mat();

        // bw = (imgray > 110) & (imgray <200) ;
        Core.compare(imGray, new Scalar(110), cond1, Core.CMP_GT);
        Core.compare(imGray, new Scalar(200), cond2, Core.CMP_LT);
        Core.bitwise_and(cond1, cond2, bw);
// END NEW ====================================

        // bw0 = imclose(bw,ones(5,5));
        Mat bw0 = new Mat();
        Imgproc.morphologyEx(bw, bw0, Imgproc.MORPH_CLOSE,
                Mat.ones(5, 5, CvType.CV_8U));

        // bw0(1,:) = 0; bw0(end,:) = 0;
        bw0.row(0).setTo(new Scalar(0));

        // can comment this line out
        // if OpenCV does not pad each edge with 1 like MATLAB's imopen() does
        bw0.row(rows - 1).setTo(new Scalar(0));
/*
// START OLD ====================================
        // bw1 = imopen(bw0,ones(round(1000/down_samp),round(50/down_samp)));
        Mat bw1 = new Mat();
        rows = Math.round(1000/ downSampling);
        cols = Math.round(50 / downSampling);
        Imgproc.morphologyEx(bw0, bw1, Imgproc.MORPH_OPEN,
                Mat.ones(rows, cols, CvType.CV_8U));

        // bw2 = bwareaopen(bw1,round(50000/down_samp^2));
        Mat bw2 = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        int points = Math.round(50000 / (downSampling * downSampling));
        Imgproc.findContours(bw1, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int numCon = contours.size();
        Mat bwColor = new Mat();
        Imgproc.cvtColor(imGray, bwColor, Imgproc.COLOR_GRAY2BGR);
// END OLD ======================================
*/

// START NEW ====================================
        //bw1 = imopen(bw0,ones(500,25));
        Mat bw1 = new Mat();

        // fix mask size to 500x25 for picture at 1040x780
        Imgproc.morphologyEx(bw0, bw1, Imgproc.MORPH_OPEN,
                Mat.ones(500, 25, CvType.CV_8U));

        //bw2 = bwareaopen(bw1,5000);
        // TODO: implement equivalent of MATLAB's bwareaopen()
        Mat bw2 = new Mat();
// END NEW ======================================

        bw2 = bw1; // skip bwareaopen for now

        Mat bwnum = new Mat();
        int nump = Imgproc.connectedComponents(bw2, bwnum);

        if (nump > 0) {

            result = new ArrayList<>();
            Point[] polePoints = new Point[nump - 1];
            double[] angles = new double[nump - 1];
            double[] distances = new double[nump - 1];
            Mat bwtmp = new Mat();

            for (int ii = 1; ii < nump; ii++) {
                Core.compare(bwnum, new Scalar(ii), bwtmp, Core.CMP_EQ);
                int nrows = bwtmp.rows();
                int ncols = bwtmp.cols();
                Mat vertical = new Mat(nrows, 1, CvType.CV_8U);
                Mat horizontal = new Mat(1, ncols, CvType.CV_8U);
                byte[] verticalBuf = new byte[nrows];
                byte[] horizontalBuf = new byte[ncols];
                int count;

                // vertical = any(bwtmp,2);
                for (int k = 0; k < nrows; k++) {
                    count = Core.countNonZero(bwtmp.row(k));
                    verticalBuf[k] = (byte) ((count != 0) ? 1 : 0);
                }
                vertical.put(0, 0, verticalBuf);

                // horizontal = any(bwtmp,1);
                for (int k = 0; k < ncols; k++) {
                    count = Core.countNonZero(bwtmp.col(k));
                    horizontalBuf[k] = (byte) ((count != 0) ? 1 : 0);
                }
                horizontal.put(0, 0, horizontalBuf);

                Mat idx = new Mat();
                Core.findNonZero(vertical, idx);
                int lastIdx = idx.rows() - 1;
                int row = (int) (idx.get(lastIdx, 0)[1]);

                Core.findNonZero(horizontal, idx);
                lastIdx = idx.rows() - 1;
                int col1 = (int) (idx.get(0, 0)[0]);
                int col2 = (int) (idx.get(lastIdx, 0)[0]);
                int col = Math.round((col1 + col2) / 2);

                double z = height * focal / ((row - vp.y) * pixelSize * downSampling);
                double x = height * Math.abs(vp.x - col) / (row - vp.y);
                double ang = Math.atan(x / z) * 180 / Math.PI;
                double dis = Math.sqrt(z * z + x * x);
                row = row + (int) (vp.y);
                polePoints[ii - 1] = new Point(col, row);
                if (vp.x > col) {
                    ang = -ang;
                }
                angles[ii - 1] = ang;
                distances[ii - 1] = dis;
            }
            result.add(polePoints);
            result.add(angles);
            result.add(distances);
        }
        return result;
    }
}
