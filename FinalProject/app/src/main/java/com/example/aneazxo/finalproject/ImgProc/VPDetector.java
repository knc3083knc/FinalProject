package com.example.aneazxo.finalproject.ImgProc;

import com.example.aneazxo.finalproject.core.Debug;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanishing point detector class.
 *
 * <p>
 * Image is gray scale.
 *
 */
public class VPDetector {

    /**
     * Construct vanishing point detector.
     *
     */
    public VPDetector() {
    }

    /**
     * Detect a vanishing point in a gray scale image.
     *
     * @param grayImage gray scale image.
     *
     * @return ArrayList of results:
     * <ul>
     * <li>[0] - (vp Point) coordinates of vanishing point (if detected).</li>
     * <li>[1] - (val double) 1 if there is a vanishing point, 0 otherwise.</li>
     * <li>[2] - (vplines Mat) pair of end points of lines use in finding
     * vanishing point.</li>
     * </ul>
     */
    public static ArrayList<Object> detect(Mat grayImage) {
        ArrayList<Object> result = new ArrayList<>();
        double val = 0;
        Point vp = null;
        Mat vpLines = null;
        Mat bw;

//        bw = edge(imgray,'canny',[0.15 0.35],1);  % ad hoc parameters
        // OpenCV's Canny does not give the same result as in Matlab.
        // In OpenCV 3.2.0, use sigma = 2 seems to be better.
        bw = edge(grayImage, "canny", 0.15, 0.35, 2);

        Mat lines = new Mat();

        int threshold = 120;
        double minLineLength = 100;
        double maxLineGap = 120;

        /*
		Using Imgproc.HoughLineP(canny, lines, rhoRes, thetaRes, threshold)
            canny - edge detected binary image
            lines - output vector of lines. Each line is represented by
                    a 4-element vector (x1,y1,x2,y2),
                    where (x1,y1) and (x2,y2) are the ending points of
                    each detected line segment.
            rhoRes - distance resolution of the accumulator in pixels
            thetaRes - angle resolution of the accumulator in radians
            threshold - the accumulator threshold, which means that only
                    the lines with more than the specified amount of
                    votes will be returned.
            minLineLength - minimum line length.
                    Line segments shorter than that are rejected.
            maxLineGap - maximum allowed gap between points on the same line
                    to link them.
         */
        Imgproc.HoughLinesP(bw, lines, 1, Math.PI / 180, threshold,
                minLineLength, maxLineGap);

        if (Debug.ON) {
            System.out.println("HoughLinesP: " + lines.rows());
        }

        int rows = lines.rows();

        if (rows < 2) {
            val = 0;
            result.add(vp);
            result.add(val);
            result.add(vpLines);
            if (Debug.ON) {
                result.add(lines);
            }
            return result;
        }

        vpLines = findVPLines(lines);

        ArrayList<Object> intersects;
        intersects = lineIntersection(vpLines);

        if (intersects == null) {
            result.add(vp);
            result.add(val);
            result.add(vpLines);
            if (Debug.ON) {
                result.add(lines);
            }
            return result;
        }

        Mat matE = (Mat) (intersects.get(0));
        Mat isConVex = (Mat) (intersects.get(3));

        Mat newE = newMatOnCondition(matE, isConVex);

        int numP = newE.cols();
        Mat crosspoint = new Mat();

        if (numP > 2) {
            Mat dist = pdist2(newE.t(), newE.t());
            Mat meandist = new Mat();
            Core.multiply(sumCol(dist), new Scalar(1. / (numP - 1)), meandist);

            // remove possible outliers
            Mat condition = new Mat();
            Core.compare(meandist, Core.mean(meandist), condition, Core.CMP_LT);
            crosspoint = newMatOnCondition(newE, condition);
        } else {
            crosspoint = newE;
        }

        if (crosspoint.rows() == 0) {
            result.add(vp);
            result.add(val);
            result.add(vpLines);
            if (Debug.ON) {
                result.add(lines);
            }
            return result;
        }

        // vanishing points is the mean value of intersection points
        Mat xy = new Mat();
        meanRow(crosspoint).convertTo(xy, CvType.CV_32S, 1, 0.5); // round up

        vp = new Point(xy.get(0, 0)[0], xy.get(1, 0)[0]);

        // if vanishing point is located at lower half of the image,
        // it is invalid
        if (vp.y > grayImage.rows() / 2.0) {
            vp = null;
        } else {
            val = 1; // found vanising point
        }

        // building result
        result.add(vp);
        result.add(val);
        result.add(vpLines);
        if (Debug.ON) {
            result.add(lines);
        }

        return result;
    }

    /**
     * Find edges.
     *
     * Detect edge using specified method (implement only OpenCV's Canny).
     *
     * @param grayImage source gray scale image.
     * @param edgeMethod edge detector method (canny).
     * @param lowThreshold edge detection low threshold.
     * @param highThreshold edge detection high threshold.
     * @param sigma the standard deviation for Gaussian filter.
     *
     * @return binary image.
     */
    private static Mat edge(Mat grayImage, String edgeMethod,
            double lowThreshold, double highThreshold, double sigma) {
        Mat result;
        /*
        http://dsp.stackexchange.com/questions/4716/differences-between-opencv-canny-and-matlab-canny
        Matlab Canny function calculates gradients using a
        derivative of Gaussian filter, the "smoothGradient()" function.
        In other words, Matlab does a Gaussian blur of the image and
        then finds the gradient of that smoothed image.

        OpenCV Canny function doesn't let you change the filter kernel it uses
        via the function parameters.
        However. You can generate the same results by first blurring the input
        image, and then passing this blurred image into the Canny function.

        To emulatate Matlab's Canny, use OpenCV's GaussianBlur() with
            - sigmaX=2 (default in Matlab)
            - filterLength = 8*ceil(sigma);

        i.e., in OpenCV, apply GuassianBlur() then Canny() to the image.
         */
        result = new Mat();
        String method = edgeMethod.toLowerCase();

        // switch/case of String is supported since Java SE 1.7
        if (method.equals("canny")) {
            int w = (int) (8 * Math.ceil(sigma)); // kernel size for Guassian blur
            Size kSize = new Size(w - 1, w - 1);

            Mat blurImage = new Mat();

            Imgproc.GaussianBlur(grayImage, blurImage, kSize, sigma, sigma);

            /*
            http://www.kerrywong.com/2009/05/07/canny-edge-detection-auto-thresholding/
                low threshold = 0.66*mean (or 0.66*median)
            high threshold = 1.33*mean (or 1.33*median)
             */
//            MatOfDouble mu = new MatOfDouble();
//            MatOfDouble stddev = new MatOfDouble();
//            Core.meanStdDev(grayImage, mu, stddev);
//            double t1 = 0.66*mu.get(0, 0)[0];
//            double t2 = 1.33*mu.get(0, 0)[0];
            // Canny recommends highThreshold = 3 * lowThreshold
            int maxValue = 255; // assume 8-bit gray scale image
            Imgproc.Canny(blurImage, result, maxValue * lowThreshold,
                    maxValue * highThreshold);
//            Imgproc.Canny(blurImage, result, 255 * lowThreshold,
//                    255 * highThreshold, 3, true);
//            Imgproc.Canny(blurImage, result, t1, t2);
        }
        //Misc.displayImage(result);
        return result;
    }

    /**
     * Find candidate lines to be used for finding vanishing point.
     *
     * @param lines resulting line segments from calling houghLinesP
     *
     * @return M-by-4 (int) Mat where M is up to 5 possible line segments to be
     * used for finding vanishing point. Each line will contain x1, y1, x2, y2.
     */
    private static Mat findVPLines(Mat lines) {
        Mat result;

        if (lines == null) {
            return null;
        }

        int rows = lines.rows();
        if (rows < 2) {
            return null;
        }

        // lines contain rows of pairs of endpoints for each line
        // so lines.channels() should be 4
        double[] angles = new double[rows];
        ArrayList<Double> listDist = new ArrayList<>();
        ArrayList<Double> listAngle = new ArrayList<>();
        ArrayList<Integer> listLinesIndex = new ArrayList<>();

        double x1, y1;
        double x2, y2;
        double[] tmpLine;
        for (int i = 0; i < rows; i++) {
            tmpLine = lines.get(i, 0);
            x1 = tmpLine[0];
            y1 = tmpLine[1];
            x2 = tmpLine[2];
            y2 = tmpLine[3];
            // round for better comparison of angles
            // so for angles within +/- 0.5 degree, use the logest line
            angles[i] = Math.round(Math.atan2(y1 - y2, x1 - x2) * 180 / Math.PI);

            // limit angle range to within [-90,90) wrt. x-axis
            if (angles[i] < -90) {
                angles[i] += 180;
            } else if (angles[i] > 89) {
                angles[i] -= 180;
            }

            // remove near horizontal and vertical lines
            // ignore vertical lines: +/-80 degrees
            // ignore horizontal lines: +/-10 degrees
            double a = Math.abs(angles[i]);
            if (a > 80 || a < 10) {
                continue;
            }

            double dx = Math.abs(x1 - x2);
            double dy = Math.abs(y1 - y2);
            double dd = dx * dx + dy * dy; // line length (for simple check)

            // insert into list only line segments with unique angles
            int idx = listAngle.indexOf(angles[i]);
            if (idx < 0) {
                // this line segment's angle is not in the list yet
                listAngle.add(angles[i]);
                listDist.add(dd);
                listLinesIndex.add(i);
            } else // this angle is already in the list,
            // change to this line segment only if it's longer than old one
            if (listDist.get(idx) < dd) {
                listDist.set(idx, dd);
                listLinesIndex.set(idx, i); // keep index of this input line
            }
        }

        int numLines = listDist.size();

        if (numLines < 2) {
            return null;
        }

        // array of distabce bewtween endpoints (x1,y1),(x2,y2) of each line
        double[] distBuf = new double[numLines];
        // array of end points(x1,y1),(x2,y2) of each line
        double[] linesBuf = new double[numLines * 4];

        for (int i = 0; i < distBuf.length; i++) {
            int idx = listLinesIndex.get(i);
            distBuf[i] = listDist.get(i);
            tmpLine = lines.get(idx, 0);

            // first point (x1,y1)
            linesBuf[i * 4] = tmpLine[0];
            linesBuf[i * 4 + 1] = tmpLine[1];

            // second point (x2,y2)
            linesBuf[i * 4 + 2] = tmpLine[2];
            linesBuf[i * 4 + 3] = tmpLine[3];
        }

        // sort (index) distance in descending order
        Mat sortedDistance = new Mat(1, distBuf.length, CvType.CV_64F);
        Mat sortedDistanceIndex = new Mat();
        sortedDistance.put(0, 0, distBuf);

        Core.sortIdx(sortedDistance, sortedDistanceIndex, Core.SORT_DESCENDING);

        // use only first 5 possible longest lines
        rows = listDist.size();
        int len = rows < 5 ? rows : 5;
        result = new Mat(len, 4, CvType.CV_32S);
        int[] vpLines = new int[len * 4];
        int[] sortedDistanceIndexBuf = new int[sortedDistanceIndex.cols()];
        sortedDistanceIndex.get(0, 0, sortedDistanceIndexBuf);
        for (int i = 0; i < len; i++) {
            int idx = sortedDistanceIndexBuf[i];

            // first point (x1,y1)
            vpLines[i * 4] = (int) linesBuf[idx * 4];
            vpLines[i * 4 + 1] = (int) linesBuf[idx * 4 + 1];

            // second point (x2,y2)
            vpLines[i * 4 + 2] = (int) linesBuf[idx * 4 + 2];
            vpLines[i * 4 + 3] = (int) linesBuf[idx * 4 + 3];
        }

        result.put(0, 0, vpLines);
        return result;
    }

    /**
     * Find intersection points from given lines.
     *
     * @param lines M-by-4 int Mat where N is up to 5 possible line segments to
     * be used for finding vanishing point. Each line will contain x1, y1, x2,
     * y2.
     *
     * @return ArrayList of results:
     * <ul>
     * <li>[0] - (E 2-by-N double Mat) x,y of intersections.</li>
     * <li>[1] - (lambda 1-by-N double Mat) lambda found.</li>
     * <li>[2] - (gamma 1-by-N double Mat) gamma found.</li>
     * <li>[3] - (isConvex 1-by-N byte Mat) is intersection on both lines?.</li>
     * </ul>
     */
    public static ArrayList<Object> lineIntersection(Mat lines) {
        ArrayList<Object> result = new ArrayList<>();

        if (lines == null) {
            return null;
        }

        int numLines = lines.rows();

        if (numLines < 2) {
            return null;
        }

        int numCombs = nChooseK(numLines, 2);

        // index of combination pair (line1, line2) to consider for intersection
        int[] line1 = new int[numCombs];
        int[] line2 = new int[numCombs];

        int i = 0, j = 0, c = 0;
        while (i < numLines) {
            j = i + 1;
            while (j < numLines) {
                line1[c] = i;
                line2[c] = j;
                c++;
                j++;
            }
            i++;
        }

        int[] linesBuf = new int[(int) (lines.total())];
        double[] bufMat = new double[numCombs * 2];
        Mat matA = new Mat(2, numCombs, CvType.CV_64F);
        Mat matB = new Mat(2, numCombs, CvType.CV_64F);
        Mat matC = new Mat(2, numCombs, CvType.CV_64F);
        Mat matD = new Mat(2, numCombs, CvType.CV_64F);

        // matrix A
        lines.get(0, 0, linesBuf);
        for (int k = 0; k < numCombs; k++) {
            bufMat[k] = linesBuf[line1[k] * 4];
            bufMat[k + numCombs] = linesBuf[line1[k] * 4 + 1];
        }
        matA.put(0, 0, bufMat);

        // matrix B
        for (int k = 0; k < numCombs; k++) {
            bufMat[k] = linesBuf[line1[k] * 4 + 2];
            bufMat[k + numCombs] = linesBuf[line1[k] * 4 + 3];
        }
        matB.put(0, 0, bufMat);

        // matrix C
        for (int k = 0; k < numCombs; k++) {
            bufMat[k] = linesBuf[line2[k] * 4];
            bufMat[k + numCombs] = linesBuf[line2[k] * 4 + 1];
        }
        matC.put(0, 0, bufMat);

        // matrix D
        for (int k = 0; k < numCombs; k++) {
            bufMat[k] = linesBuf[line2[k] * 4 + 2];
            bufMat[k + numCombs] = linesBuf[line2[k] * 4 + 3];
        }
        matD.put(0, 0, bufMat);

        Mat matF1 = new Mat();
        Mat matF2 = new Mat();
        Mat matM11 = new Mat();
        Mat matM12 = new Mat();
        Mat matM21 = new Mat();
        Mat matM22 = new Mat();
        Mat deter = new Mat();
        Mat lambda = new Mat();
        Mat gamma = new Mat();
        Mat matE = new Mat();

        // F1 = B(1,:)-D(1,:);
        Core.subtract(matB.row(0), matD.row(0), matF1);

        // F2 = B(2,:)-D(2,:);
        Core.subtract(matB.row(1), matD.row(1), matF2);

        // M11 = B(1,:)-A(1,:);
        Core.subtract(matB.row(0), matA.row(0), matM11);

        // M21 = B(2,:)-A(2,:);
        Core.subtract(matB.row(1), matA.row(1), matM21);

        // M12 = C(1,:)-D(1,:);
        Core.subtract(matC.row(0), matD.row(0), matM12);

        // M22 = C(2,:)-D(2,:);
        Core.subtract(matC.row(1), matD.row(1), matM22);

        // should never have parallel lines,
        // then deter will never be 0 => no need to check for deter = 0
        // deter = M11.*M22 - M12.*M21;
        Core.subtract(matM11.mul(matM22), matM12.mul(matM21), deter);

        // lambda = -(F2.*M12-F1.*M22)./deter;
        Mat tmpMat = new Mat();
        Core.subtract(matF2.mul(matM12), matF1.mul(matM22), tmpMat);
        Core.divide(tmpMat, deter, lambda, -1);

        // gamma = (F2.*M11-F1.*M21)./deter;
        Core.subtract(matF2.mul(matM11), matF1.mul(matM21), tmpMat);
        Core.divide(tmpMat, deter, gamma);

        // E = ([1;1]*lambda).*A + ([1;1]*(1-lambda)).*B;
        Mat oneMat = new Mat(2, 1, CvType.CV_64F);
        double[] constBuf = new double[]{1, 1};
        Mat tmpM1 = new Mat();
        Mat tmpM2 = new Mat();
        oneMat.put(0, 0, constBuf);

        // tmpM1 = ([1;1]*lambda).*A
        Core.gemm(oneMat, lambda, 1, new Mat(), 0, tmpM1);
        tmpM1 = tmpM1.mul(matA);

        // tmpM2 = ([1;1]*(1-lambda)).*B
        Core.multiply(lambda, new Scalar(-1), tmpM2);
        Core.add(tmpM2, new Scalar(1), tmpM2);
        Core.gemm(oneMat, tmpM2, 1, new Mat(), 0, tmpM2);
        tmpM2 = tmpM2.mul(matB);

        // tmpMat = [1;1]*lambda).*A + ([1;1]*(1-lambda)).*B
        Core.add(tmpM1, tmpM2, matE);

        Mat isConvex = new Mat();
        Mat checkLamda = new Mat();
        Mat checkGamma = new Mat();
        // tmpM1 = 0 <= lambda
        Core.compare(lambda, new Scalar(0), tmpM1, Core.CMP_GE);
        // tmpM2 = lambda <= 1
        Core.compare(lambda, new Scalar(1), tmpM2, Core.CMP_LE);
        // checkLambda = (0 <= lambda & lambda <= 1)
        Core.bitwise_and(tmpM1, tmpM2, checkLamda);

        // tmpM1 = 0 <= gamma
        Core.compare(gamma, new Scalar(0), tmpM1, Core.CMP_GE);
        // tmpM2 = gamma <= 1
        Core.compare(gamma, new Scalar(1), tmpM2, Core.CMP_LE);
        // checkGamma = (0 <= gamma & gamma <= 1)
        Core.bitwise_and(tmpM1, tmpM2, checkGamma);

        Core.bitwise_and(checkLamda, checkGamma, isConvex);

        // Build result list
        result.add(matE);
        result.add(lambda);
        result.add(gamma);
        result.add(isConvex);

        return result;
    }

    private static int nChooseK(int n, int k) {
        if (k > n) {
            return 0;
        }
        if (k > n / 2) {
            k = n - k;
        }
        double product = 1;
        double kk = 1;
        for (int i = 0; i < k; i++) {
            product *= n / kk;
            kk += 1;
            n -= 1;
        }
        return (int) product;
    }

    /**
     * Return new Mat based on nonzero element in mask.
     *
     * @param m M-by-N Mat source matrix.
     * @param mask 1-by-N byte vector containing mask (255 or 0).
     *
     * @return M-by-P Mat where P is number of nonzero member in mask.
     */
    private static Mat newMatOnCondition(Mat m, Mat mask) {
        Mat result;
        int cols = mask.cols();

        byte[] bufBytes = new byte[cols];
        mask.get(0, 0, bufBytes);

        List<Mat> listMat = new ArrayList<>();

        for (int i = 0; i < cols; i++) {
            if (bufBytes[i] != 0) {
                listMat.add(m.col(i));
            }
        }
        result = new Mat();
        Core.hconcat(listMat, result);
        return result;
    }

    /**
     * Emulate Matlab's pdist2 for column vectors of points.
     *
     * @param p1 first vector of pints.
     * @param p2 second vector of points.
     *
     * @return Euclidian's distance between all pairs of points from p1 to p2.
     */
    private static Mat pdist2(Mat p1, Mat p2) {
        Mat result;

        Mat tmpMat;
        List<Mat> listMat = new ArrayList<>();

        int rows = p1.rows();

        for (int i = 0; i < rows; i++) {
            tmpMat = distanceToPoint2Ds(p1.row(i), p2);
            listMat.add(tmpMat);
        }

        result = new Mat();
        Core.hconcat(listMat, result);

        return result;
    }

    private static Mat distanceToPoint2Ds(Mat pt, Mat points) {
        Mat result;

        int rows = points.rows();

        Mat tmpMat = Mat.zeros(rows, 2, points.type());
        for (int i = 0; i < rows; i++) {
            pt.copyTo(tmpMat.row(i));
        }

        result = new Mat();
        Core.subtract(tmpMat, points, tmpMat);
        Core.magnitude(tmpMat.col(0), tmpMat.col(1), result);

        return result;
    }

    /**
     * Circular shift rows up/down.
     *
     * If n > 0, shift down, if n ;&lt 0 shift up.
     *
     * @param m source matrix.
     * @param n number of times to shift. n > 0 shift down, n ;&lt 0 shift up.
     *
     * @return shifted matrix, up or down.
     */
    private static Mat shiftRows(Mat m, int n) {
        Mat result;
        int rows = m.rows();
        int k;

        k = n % rows;
        if (k == 0) {
            return m;
        }

        result = m.clone();

        if (n > 0) {
            int start1, end1, start2, end2;
            start1 = 0;
            end1 = rows - k;
            start2 = k;
            end2 = rows;
            m.rowRange(start1, end1).copyTo(result.rowRange(start2, end2));

            start1 = rows - k;
            end1 = rows;
            start2 = 0;
            end2 = k;
            m.rowRange(start1, end1).copyTo(result.rowRange(start2, end2));
        } else {
            n = -n;
            k = n % rows;
            int start1, end1, start2, end2;
            start1 = 0;
            end1 = k;
            start2 = rows - k;
            end2 = rows;
            m.rowRange(start1, end1).copyTo(result.rowRange(start2, end2));

            start1 = k;
            end1 = rows;
            start2 = 0;
            end2 = rows - k;
            m.rowRange(start1, end1).copyTo(result.rowRange(start2, end2));

        }

        return result;
    }

    /**
     * Find sum along column.
     *
     * @param m source matrix.
     *
     * @return 1-by-N Mat where N is number columns in matrix m.
     */
    private static Mat sumCol(Mat m) {
        Mat result;

        int cols = m.cols();

        result = Mat.zeros(1, cols, m.type());

        for (int i = 0; i < cols; i++) {
            Core.add(result, m.row(i), result);
        }

        return result;
    }

    /**
     * Find mean along column.
     *
     * @param m source matrix.
     *
     * @return 1-by-N Mat where N is number of columns in matrix m.
     */
    private static Mat meanCol(Mat m) {
        Mat result;

        int cols = m.cols();

        List<Mat> listMat = new ArrayList<>();

        for (int i = 0; i < cols; i++) {
            listMat.add(new Mat(1, 1, CvType.CV_64F, Core.mean(m.col(i))));
        }
        result = new Mat();
        Core.hconcat(listMat, result);

        return result;
    }

    /**
     * Find mean along row.
     *
     * @param m source matrix.
     *
     * @return M-by-1 Mat where M is number of rows in matrix m.
     */
    private static Mat meanRow(Mat m) {
        Mat result;

        int rows = m.rows();

        List<Mat> listMat = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            listMat.add(new Mat(1, 1, CvType.CV_64F, Core.mean(m.row(i))));
        }
        result = new Mat();
        Core.vconcat(listMat, result);

        return result;
    }

    /**
     * Draw lines from given end points (lines).
     *
     * @param img source image to draw lines.
     * @param lines line containing x1, y1, x2, y2.
     * @param color RGB in Scalar.
     * @param thickness line thickness in pixel.
     */
    public static void drawLines(Mat img, Mat lines, Scalar color,
            int thickness) {
        int rows = lines.rows();
        if (rows == 0) {
            return;
        }

        // lines contain rows of pairs of endpoints for each line
        // so lines.channels() should be 4
        Point pt1 = new Point();
        Point pt2 = new Point();
        int[] line = new int[4];
        for (int i = 0; i < rows; i++) {
            double angle;
            lines.row(i).get(0, 0, line);
            pt1.x = line[0];
            pt1.y = line[1];
            pt2.x = line[2];
            pt2.y = line[3];
            Imgproc.line(img, pt1, pt2, color, thickness);
            angle = Math.atan2(pt1.y - pt2.y, pt1.x - pt2.x) * 180 / Math.PI;
            if (Debug.ON) {
                System.out.println("Line: " + angle);
            }
        }
    }
}
