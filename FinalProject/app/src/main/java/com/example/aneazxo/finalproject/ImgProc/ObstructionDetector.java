package com.example.aneazxo.finalproject.ImgProc;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Obstruction detector class.
 * <p>
 * <p>
 * Image is gray scale.
 */
public class ObstructionDetector {

    /**
     * Construct obstruction detector.
     */
    public ObstructionDetector() {

    }

    /**
     * Detect whether there is any detect (poles) right in front of you.
     * <p>
     * <p>
     * Perform on gray scale image.
     *
     * @param grayImage gray scale image
     * @return 1 if there is an detect, 0 otherwise
     */
    public static int detect(Mat grayImage) {
        Mat out1;
        Mat out2;
        int stop;

        // Note: OpenCV uses population standard deviation (divide by N)
        out1 = variance(grayImage); // test the variance along the column
//        out2 = compare(out1, 500, Core.CMP_LT); // 500 is ad hoc number
        out2 = new Mat();
        Imgproc.threshold(out1, out2, 500, 1, Imgproc.THRESH_BINARY_INV);

        Range rowRange, colRange;
        rowRange = new Range(0, 1);      // rows [0, 1)
        colRange = new Range(100, 681);  // cols [100, 681)
        Mat temp = new Mat(out2, rowRange, colRange);  // out2(100:680)

        // ad hoc parameters, look for obstructions in the middle of image (columns 100-680)
        // det1 = imerode(out2(100:680),ones(1,10));
        // ad hoc parameters,
        // look for obstructions in the middle of image (columns 100-680)
        Mat det1 = erode(temp, Mat.ones(1, 10, CvType.CV_16S));

        stop = 0;

        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(det1);

        if (minMaxLoc.maxVal == 1) {
            stop = 1;
            System.out.println("Obstruction in front of you");
        }
        return stop;
    }

    /**
     * Find variance along column of a gray scale image.
     * <p>
     * <p>
     * Use int to store data since Java does not have unsigned byte.
     *
     * @param grayImage gray scale image to process
     * @return (1 x n) double Mat of variance in each column
     */
    private static Mat variance(Mat grayImage) {
        Mat result;

        // OpenCV uses population variance (divide by N)
        Mat im = new Mat();

        int rows = grayImage.rows();
        int cols = grayImage.cols();

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();

        double[] nVar = new double[cols];
        double v;
        for (int i = 0; i < cols; i++) {
            Core.meanStdDev(grayImage.col(i), mean, stddev);
            v = stddev.get(0, 0)[0];
//            nVar[i] = (v * v) * rows / (rows - 1);
            nVar[i] = (v * v); // use population variance
        }
        // packing result
        result = new Mat(1, cols, CvType.CV_64F);
        result.put(0, 0, nVar);

        return result;
    }

    /**
     * Compare each column in src1 (1 x n) Mat with src2 (double).
     * <p>
     * <p>
     * Return (1 x n) Mat with values of either 1 or 0 that results from
     * comparing each column in src1 with value of src2.
     *
     * @param src1            (1 x n) Mat for comparison.
     * @param src2            number to compare with.
     * @param compareOpration OpenCV comparison operation type (CmpTypes).
     * @return (1 x n) logical Mat (contains either 1 or 0)
     */
    public static Mat compare(Mat src1, double src2, int compareOpration) {

        Mat result;
        int cols = src1.cols();
        Scalar s2 = new Scalar(src2);

        // destination matrix
        // OpenCV specifies destination for comparison must have type CV_8U
        Mat mask = new Mat(1, cols, CvType.CV_8U);

        // result of comparison will be either 0 or 255 and saved in mask
        Core.compare(src1, s2, mask, compareOpration);

        short[] buf = new short[cols];
        byte[] maskBuf = new byte[cols];
        mask.get(0, 0, maskBuf);  // read from mask into maskBuf
        for (int i = 0; i < cols; i++) {
            if (maskBuf[i] == 0) {
                buf[i] = 0;
            } else {
                buf[i] = 1;
            }
        }

        // packing result
        result = new Mat(1, cols, CvType.CV_16S);
        result.put(0, 0, buf);

        return result;
    }

    /**
     * Image erosion.
     * <p>
     * <p>
     * OpenCV support erosion for source image with type: CV_8U, CV_16U, CV_16S,
     * CV_32F or CV_64F.
     *
     * @param src    source (1 x n) short Mat.
     * @param kernel structural element (1 x c).
     * @return (1 x n) int Mat.
     */
    public static Mat erode(Mat src, Mat kernel) {
        Mat result;
        result = new Mat(1, src.cols(), CvType.CV_32S);
        Imgproc.erode(src, result, kernel);
        return result;
    }

}
