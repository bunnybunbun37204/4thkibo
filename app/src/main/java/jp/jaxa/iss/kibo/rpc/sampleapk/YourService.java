package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import java.util.List;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;



import org.opencv.core.Mat;


import android.graphics.Bitmap;
import android.util.Log;

//Zxing lib
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;
import org.opencv.aruco.*;
import  org.opencv.core.Scalar;
import org.opencv.calib3d.Calib3d;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import static org.opencv.android.Utils.matToBitmap;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {
    private imgProcessing imgProc;
    @Override
    protected void runPlan1(){
        // the mission starts
        api.startMission();
        int loop_counter = 0;

        while (true){
            // get the list of active target id
            List<Integer> list = api.getActiveTargets();
            Log.i("TARGET INFO DEBUG", list.toString());

            // move to a point
            // move to Point 4

            moveToPoint(1);
            Log.i("INFO_MOVING", "Move to point 1");

            // irradiate the laser
            api.laserControl(true);
            Log.i("LOG-DEBUGGER", "LASER ON");
            api.laserControl(true);

            // take active target snapshots
            int target_id = 4;
            api.takeTargetSnapshot(target_id);

            api.laserControl(false);

            list = api.getActiveTargets();
            Log.i("LOG-DEBUGGER", "LIST : "+list.toString());

            if (target_id == 4) {
                Log.i("LOG-INFO", "TARGET");
                break;
            }

            /* ************************************************ */
            /* write your own code and repair the ammonia leak! */
            /* ************************************************ */

            // get remaining active time and mission time
            List<Long> timeRemaining = api.getTimeRemaining();
            Log.i("LOG-DEBUGGER", timeRemaining.toString());

            // check the remaining milliseconds of mission time
            if (timeRemaining.get(1) < 60000){
                break;
            }

            loop_counter++;
            if (loop_counter == 2){
                break;
            }
        }
        // turn on the front flash light
        api.flashlightControlFront(0.05f);
        Log.i("LOG_INFO", "TURN ON");
        // get QR code content
        moveToPoint(7);
        Log.i("LOG-INFO", "moving the bee");
        String mQrContent = readQRImage2(api.getMatNavCam(), 250);
        Log.d("LOG_DEBUGGER", mQrContent);

        // turn off the front flash light
        api.flashlightControlFront(0.00f);

        // notify that astrobee is heading to the goal
        api.notifyGoingToGoal();

        /* ********************************************************** */
        /* write your own code to move Astrobee to the goal positiion */
        /* ********************************************************** */

        // send mission completion
        api.reportMissionCompletion(mQrContent);
    }

    @Override
    protected void runPlan2(){
       // write your plan 2 here
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here
    }

    // You can add your method
    public String readQRImage2(Mat mat, int thresh) {
        String contents = " ";

        int key = 0;
        while (contents == " "){
            Map<DecodeHintType,Object> hint = new HashMap<>();
            hint.put(DecodeHintType.PURE_BARCODE, true);
            Log.d("LOG-DEBUGGER","DEFINED HINT : "+hint.toString());
            imgProc = new imgProcessing();
            imgProc.findRectContours(mat, thresh);
            Mat src_mat;
            if(key >= 1){
                src_mat = api.getMatNavCam();
            }
            else if(!imgProc.STATUS){
                src_mat = mat;
            }
            else {
                src_mat = imgProc.sharpenImg;
            }
            Log.d("LOG-DEBUGGER","MAT COLS IS "+src_mat.cols()+" MAT ROWS IS "+src_mat.rows());
            Bitmap bMap = Bitmap.createBitmap(src_mat.cols(), src_mat.rows(), Bitmap.Config.ARGB_8888);

            matToBitmap(src_mat,bMap);
            Log.d("LOG-DEBUGGER","CONVERT MAT TO BITMAP");
            Log.d("LOG-DEBUGGER","get new bitmap!!!");
            int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new QRCodeReader();
            try {
                Log.d("LOG-DEBUGGER", "START DECODE");
                //contents = reader.decode(bitmap, hint).getText();
                contents = reader.decode(bitmap).getText();
                Log.d("LOG-DEBUGGER", "FINISH DECODE");
            } catch (NotFoundException e) {
                e.printStackTrace();
                key++;
            } catch (ChecksumException e) {
                e.printStackTrace();
                key++;
            } catch (FormatException e) {
                e.printStackTrace();
                key++;
            }
        }


        Log.d("LOG-DEBUGGER", "END OF SCAN");
        //ARUCOREADER(imgProc.processedImg);
        return contents;
    }

    private void moveToPoint(int point) {
        switch (point) {
            case 1:
                moveToWrapper(10.51, -6.7185, 5.1804, 0, 0, -1, 0);
                break;
            case 2:
                moveToWrapper(10.612, -9.0709, 4.48, 0.5, 0.5, -0.5, 0.5);
                break;
            case 3:
                moveToWrapper(10.71, -7.7, 4.48,
                        0, 0.707, 0, 0.707);
                break;
            case 4:
                moveToWrapper(11.2746, -9.92284, 5.2988, 0, 0, -0.707, 0.707);
                break;
            case 5:
                moveToWrapper(11.114, -7.9756, 5.3393, -0.5, -0.5, -0.5, 0.5);
                break;
            case 6:
                moveToWrapper(11.355, -8.9929, 4.7818, 0, 0, 0, 1);
                break;
            case 7:
                moveToWrapper(11.369, -8.5518, 4.48, 0, 0.707, 0, 0.707);
                break;
        }
    }


    // You can add your method
    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w) {

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float) qua_x, (float) qua_y,
                (float) qua_z, (float) qua_w);

        Result result = api.moveTo(point, quaternion, true);
        System.out.println("LOG-DEBUGGER "+result.hasSucceeded());

        int loopCounter = 0;
        while (!result.hasSucceeded() || loopCounter < LOOP_MAX) {
            result = api.moveTo(point, quaternion, true);
            ++loopCounter;
        }
    }
}
