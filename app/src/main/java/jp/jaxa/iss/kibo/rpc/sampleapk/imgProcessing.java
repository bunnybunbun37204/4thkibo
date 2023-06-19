package jp.jaxa.iss.kibo.rpc.sampleapk;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Rect;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import java.lang.Math;

public class imgProcessing {
    public Mat processedImg;
    public Mat processedCircleImg;
    public static Mat threshImg;
    public static Mat grayImg;
    public Mat sharpenImg;
    private static int kernelSize=5;
    private static Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2 * kernelSize + 1, 2 * kernelSize + 1),
            new Point(kernelSize, kernelSize));
    public static Mat cropped_img;
    public Mat warped_img;
    public Point text_position;
    public Rect target_rect;
    public static boolean STATUS = true;

    private static Point[] sortingPoints(MatOfPoint pts,int x,int y) {
        Point[] sortedPoints = new Point[4];
        double data[];
        for(int i=0; i<pts.size().height; i++){
            data = pts.get(i,0);
            double datax = data[0];
            double datay = data[1];
//		    0-------1
//		    |		|
//		    |  x,y  |
//		    |		|
//		    2-------3
            if(datax < x && datay < y){
                sortedPoints[0]=new Point(datax,datay);
            }else if(datax > x && datay < y){
                sortedPoints[1]=new Point(datax,datay);
            }else if (datax < x && datay > y){
                sortedPoints[2]=new Point(datax,datay);
            }else if (datax > x && datay > y){
                sortedPoints[3]=new Point(datax,datay);
            }
        }
        return sortedPoints;

    }

    private static Mat sharpeningImg(Mat src) {
        Mat dst = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.medianBlur(src, dst,7);
        Core.subtract(src, dst, dst);
        Core.add(dst, src, dst);

        return dst;
    }

    public static Bitmap GreyScale(Bitmap bitmap){
        Mat mat1 = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
        bitmapToMat(bitmap,mat1);
        Mat mat2 = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        Log.d("LOG-DEBUGGER","GARYIMG WAS DECLARED");
        Imgproc.cvtColor(mat1, mat2, Imgproc.COLOR_RGB2GRAY);
        Log.d("LOG-DEBUGGER","CONVERT FINISH");

        Bitmap result = Bitmap.createBitmap(mat2.cols(), mat2.rows(), Bitmap.Config.ARGB_8888);
        return  result;
    }



    private static Mat thresholding(Mat img, int thresh) {
        Mat gray = new Mat(img.rows(), img.cols(), img.type());
        try {
            // Imgproc.cvtColor(img, gray, Imgproc.COLOR_BayerBG2GRAY);
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BayerRG2GRAY);
        }
        catch (Exception e){
            Log.d("LOG-DEBUGGER","ERR : "+e.getMessage());
        }
        Mat binaryImg = new Mat(img.rows(), img.cols(), img.type(), new Scalar(0));
        Imgproc.threshold(gray, binaryImg, thresh, 255, Imgproc.THRESH_BINARY);
        Imgproc.erode(binaryImg, binaryImg, element);
        Log.d("LOG-DEBUGGER","ERODE FINISH");
        return binaryImg;
    }

    private static Mat thresholdingImg(Mat gray) {
        Mat binaryImg = new Mat(gray.rows(), gray.cols(), gray.type(), new Scalar(0));
        Log.d("LOG-DEBUGGER","BINARYIMGTHRS WAS DECLARED "+gray.type());
        try {
            Imgproc.threshold(gray, binaryImg,250, 255, Imgproc.THRESH_BINARY);
            Log.d("LOG-D","IMAGE WAS THRESHOLDED");
        }
        catch (Exception e){
            String err = e.getMessage();
            Log.d("LOG-DEBUGGER","ERR IS "+err);
        }
        Log.d("LOG-DEBUGGER","ZAWARUDO TYPE GRAY SOURCE : "+gray.type());
        Imgproc.erode(binaryImg, binaryImg, element);
        Log.d("LOG-DEBUGGER","FINISH THRESHOLDING");
        return binaryImg;
    }

    private static Mat thresholdingBmap(Bitmap bitmap) {
        Mat mat1 = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
        bitmapToMat(bitmap, mat1);
        Log.d("LOG-DEBUGGER","START THRESHOLDING");
        Mat mat2 = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        Imgproc.cvtColor(mat1, mat2, Imgproc.COLOR_RGB2GRAY);
        Log.d("LOG-DEBUGGER","GARYIMG WAS DECLARED");

        Mat binaryImg = new Mat(mat1.rows(), mat1.cols(), mat1.type(), new Scalar(0));
        Log.d("LOG-DEBUGGER","BINARYIMGTHRS WAS DECLARED "+binaryImg.type());
        Imgproc.threshold(mat2, binaryImg, 250, 255, Imgproc.THRESH_BINARY);
        Imgproc.erode(binaryImg, binaryImg, element);
        Log.d("LOG-DEBUGGER","FINISH THRESHOLDING");
        return binaryImg;
    }

    public void findCircularContours(Mat img) {
        processedCircleImg = new Mat(img.rows(), img.cols(), img.type());
        img.copyTo(processedCircleImg);

        grayImg = new Mat();
        //Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayImg, grayImg, new Size(7, 7),1.5);

        Mat circles = new Mat();


        Imgproc.HoughCircles(grayImg, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 10, 100, 20, 1, 10);
        System.out.println("c : "+circles.size());
        for(int i=0;i<circles.cols();i++) {
            double[] data = circles.get(0, i);
            int r = (int) Math.round(data[2]);

            Point center = new Point(Math.round(data[0]),Math.round(data[1]));
            // circle center
            Imgproc.circle( processedCircleImg, center, 3, new Scalar(0,255,0), -1);
            // circle outline
            Imgproc.circle( processedCircleImg, center, r, new Scalar(0,0,255), 1);

        }
//	    List<MatOfPoint> contours = new ArrayList<>();
//	    Mat hierarchey = new Mat();
//	    Imgproc.findContours(binImg, contours, hierarchey, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

    }

    public void findRectContours(Mat img, int thresh) {
        Log.d("LOG-DEBUGGER","START FINDRECT");
        processedImg = new Mat(img.rows(), img.cols(),img.type());
        Log.d("LOG-DEBUGGER","PROCESSEDIMG WAS DECLARED "+processedImg.type());
        img.copyTo(processedImg);
        Mat binImg = thresholding(processedImg, thresh);
        Log.d("LOG-DEBUGGER","BINARYIMG WAS DECLARED");
        threshImg = new Mat(img.rows(), img.cols(), img.type(), new Scalar(0));
        Log.d("LOG-DEBUGGER","THRESIMG WAS DECLARED");
        binImg.copyTo(threshImg);
        Log.d("LOG-DEBUGGER","COPY TO BINIMG FINISH");
        List<MatOfPoint> contours = new ArrayList<>();
        Log.d("LOG-DEBUGGER","CONTOURS WAS DECLARED");
        Mat hierarchey = new Mat();
        Log.d("LOG-DEBUGGER","HIERARCHY WAS DECLARED");
        try {
            Imgproc.findContours(binImg, contours, hierarchey, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        }
        catch (Exception e){
            String err = e.getMessage();
            Log.d("LOG-DEBUGGER","ERR IS "+err);
            Imgproc.findContours(binImg, contours, hierarchey, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        }
        int i;
        Log.d("LOG-DEBUGGER","CURRENT CONTOUR SIZE IS : "+contours.size());

        if(contours.size()<=3){
            STATUS = false;
            Log.d("[STATUS]","ERR let's use common scanner");
            return;
        }
        for (i = 0; i < contours.size(); i++) {
            Log.d("LOG-DEBUGGER","STARTING LOOP : "+i);
            //Drawing Contours
            if(hierarchey.get(0, i)[2]==-1.0) {
                MatOfPoint2f ct2f = new MatOfPoint2f( contours.get(i).toArray() );
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                Moments moment = Imgproc.moments(ct2f);
                Log.d("LOG-DEBUGGER","DEFINED ESSENTIAL VARIABLES");

                int x = (int) (moment.get_m10() / moment.get_m00());
                int y = (int) (moment.get_m01() / moment.get_m00());

                Log.d("LOG-DEBUGGER","DEFINED X Y VARIABLES");


                double approxDistance = Imgproc.arcLength(ct2f, true)*0.1;
                Imgproc.approxPolyDP(ct2f, approxCurve, approxDistance, true);
                MatOfPoint points = new MatOfPoint( approxCurve.toArray() );
                Log.d("LOG-DEBUGGER","POINT SIZE HEIGHT IS "+points.size().height);
                if(points.size().height==4.0) {
                    target_rect = Imgproc.boundingRect(points);

                    text_position=new Point(target_rect.x,target_rect.y);
                    cropped_img=new Mat();
                    cropped_img=img.submat(target_rect);
                    Log.i("[INFO]","POINT : "+points.width()+","+points.height()+" X : "+x+" Y : "+y);
                    Point[] sorted_pts=sortingPoints(points, x, y);
                    Log.d("LOG-DEBUGGER","START SORT PTS"+sorted_pts.length);
                    if(sorted_pts.length == 0 || sorted_pts==null){
                        STATUS = false;
                        Log.d("[STATUS]","SORTPTS");
                        return;
                    }
                    for(int j = 0; j < sorted_pts.length; j++){
                        Log.d("[INFO]","X : "+sorted_pts[j].x+" Y : "+sorted_pts[j].y);
                    }
//                    for(int j=0;j<sorted_pts.length;j++) {
//                        Log.d("LOG-DEBUGGER","START SORT LOOP");
//                        Point p = new Point(sorted_pts[j].x,sorted_pts[j].y);
//                        Imgproc.circle(processedImg, p, 5, new Scalar(255,0,0), -1);
//                    }
                    MatOfPoint2f src_pts=new MatOfPoint2f();
                    src_pts.fromArray(sorted_pts);
                    Log.d("LOG-DEBUGGER","FINSIH ARRAY FORMING : "+src_pts.toString());

                    double w1=Math.sqrt(Math.pow((sorted_pts[1].x-sorted_pts[0].x), 2)+
                            Math.pow((sorted_pts[1].y-sorted_pts[0].y), 2));
                    double w2=Math.sqrt(Math.pow((sorted_pts[3].x-sorted_pts[2].x), 2)+
                            Math.pow((sorted_pts[3].y-sorted_pts[2].y), 2));

                    double h1=Math.sqrt(Math.pow((sorted_pts[1].x-sorted_pts[3].x), 2)+
                            Math.pow((sorted_pts[1].y-sorted_pts[3].y), 2));
                    double h2=Math.sqrt(Math.pow((sorted_pts[0].x-sorted_pts[2].x), 2)+
                            Math.pow((sorted_pts[0].y-sorted_pts[2].y), 2));

                    double max_w=Math.max(w1,w2);
                    double max_h=Math.max(h1,h2);


                    MatOfPoint2f dst_pts=new MatOfPoint2f(
                            new Point(0, 0),
                            new Point(max_w-1,0),
                            new Point(0,max_h-1),
                            new Point(max_w-1,max_h-1)
                    );
                    Mat perspective_tf = Imgproc.getPerspectiveTransform(src_pts, dst_pts);
                    Log.d("LOG-DEBUGGER","DEFINED PERSPECTIVE : "+perspective_tf.channels());
                    if(perspective_tf == null){
                        Log.d("LOG-DEBUGGER","NULL");
                        STATUS = false;
                        return;
                    }
                    warped_img = new Mat();
                    Imgproc.warpPerspective(img, warped_img, perspective_tf, new Size(max_w,max_h), Imgproc.INTER_LINEAR);
                    sharpenImg = new Mat();
                    sharpenImg=sharpeningImg(warped_img);
                    Log.d("LOG-DEBUGGER","SHARPENIMG IS "+sharpenImg.size().toString());
//			         Imgproc.threshold(warped_img, warped_img, 180, 255, Imgproc.THRESH_BINARY);
//			         Imgproc.rectangle (processedImg, target_rect, color, 1);
//                    Imgproc.drawContours(processedImg, contours, i, color, 2, Imgproc.LINE_8, hierarchey, 1, new Point() ) ;
                    Log.d("LOG-DEBUGGER","FINSIH FINDRECT");
                }
                else if(i == contours.size()-1 && points.height() != 4){
                    STATUS = false;
                    return;
                }
            }
        }
        Log.d("LOG-DEBUGGER","OUT OF LOOP : "+i);
        Log.d("[STATUS]",""+STATUS);
    }
}