package org.firstinspires.ftc.teamcode.robot.commands.auto;

import static org.firstinspires.ftc.teamcode.ug_refrence.CommandDrive.TRIGGER_THRESHOLD;

import androidx.core.math.MathUtils;

import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.util.MathUtil;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.sun.tools.javac.util.Pair;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.swampbots_util.DuckPatternPipeline;
import org.firstinspires.ftc.teamcode.swampbots_util.DuckPlacement;
import org.firstinspires.ftc.teamcode.swampbots_util.GamepadCooldowns;
import org.firstinspires.ftc.teamcode.robot.subsystems.Camera;
import org.firstinspires.ftc.teamcode.swampbots_util.SwampbotsUtil;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

//bot 313.23    left 193.12 right 308.82    top 182.74  bound 253.48

public class AutoCameraControl {
    private Camera camera;
    private Gamepad gamepad1;
    private Gamepad gamepad2;
    private MultipleTelemetry multiTelemetry;
    private Telemetry telemetry;

    private SwampbotsUtil util;

    private GamepadCooldowns gp1;
    private GamepadCooldowns gp2;
    private double runtime = 0.0;

    // Toggle for output overlays   [type, enabled?]
    // Toggle for output overlays [Target Col, Rect, Point, HSV]
    private final boolean[] overlays = {false, true, true, false};
    private int togglePoint = 0;
    // Cooldown in the order: a, b, x, y
    private final boolean[] buttonCooldown = {false, false, false, false};

    private boolean stopPointTelemetry = true;//TODO: Make false


    public static final double THRESHOLD_STEP = 0.04;

    public static final double HUE_MAX = 180.0;
    public static final double SAT_MAX = 255.0;
    public static final double VAL_MAX = 255.0;
    public static final double HSV_MIN = 0.0;

    public static final double RECT_STEP = 0.04;
    public static final double RECT_MIN = 0.0;

    public static final int IMG_WIDTH = 640;
    public static final int IMG_HEIGHT = 480;

    private List<MatOfPoint> contours;
    private final double CONTOUR_THRESHOLD = 100;
    private DuckPlacement placement;
    private ArrayList<Pair<DuckPlacement, Double>> fullPlacementData = new ArrayList<Pair<DuckPlacement, Double>>();
    private final int maxPlacementDataSize = 10;

    public double Bound1Percent = 15.0;
    public double Bound2Percent = 54.69;
    public double Bound3Percent = 95.0;

    private ElapsedTime timer;

    public AutoCameraControl(Camera camera, Gamepad gamepad1, Gamepad gamepad2, MultipleTelemetry multiTelemetry){
        this.camera = camera;
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
        this.multiTelemetry = multiTelemetry;

        gp1 = new GamepadCooldowns();
        gp2 = new GamepadCooldowns();

        timer = new ElapsedTime();
        timer.reset();

        util = new SwampbotsUtil();

    }

    public AutoCameraControl(Camera camera, Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry){
        this.camera = camera;
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
        this.telemetry = telemetry;

        gp1 = new GamepadCooldowns();
        gp2 = new GamepadCooldowns();


        timer = new ElapsedTime();
        timer.reset();

        util = new SwampbotsUtil();

    }

    public AutoCameraControl(Camera camera, Gamepad gamepad1, Gamepad gamepad2) {
        this(camera, gamepad1, gamepad2, null);
    }

     public void periodic() {
        // Update runtime read
        runtime = new Date().getTime(); //TODO: Make less jank?

         // Update local HSV threshold references
         double[] localHsvHue = camera.getHsvHue();
         double[] localHsvSat = camera.getHsvSat();
         double[] localHsvVal = camera.getHsvVal();

        //--------------------------------------------------------------------------------------
        // START HSV THRESHOLD CONTROLS
        //--------------------------------------------------------------------------------------

            /*
                CONTROLS: (increase, decrease)
                Hue min: gp1.up,    gp1.down
                Hue max: gp1.y,     gp1.a
                Sat min: gp1.right, gp1.left
                Sat max: gp1.b,     gp1.x
                Val min: gp1.lb,    gp1.lt
                Val max: gp1.rb,    gp1.rt
             */

        // Modify threshold variables if the buttons are pressed and thresholds are within outer limits 0 & 255

        // HUE MINIMUM
        if (gamepad1.dpad_down && gp1.dpDown.ready(runtime)) {
            if (localHsvHue[0] > HSV_MIN)
                camera.setHsvHueMin(localHsvHue[0] - THRESHOLD_STEP)   /*hsvHue[0] -= THRESHOLD_STEP*/;
            else
                camera.setHsvHueMin(HSV_MIN)                           /*hsvHue[0] = HSV_MIN*/;
            gp1.dpDown.updateSnapshot(runtime);
        }

        if (gamepad1.dpad_up && gp1.dpUp.ready(runtime)) {
            if (localHsvHue[0] < localHsvHue[1])
                camera.setHsvHueMin(localHsvHue[0] + THRESHOLD_STEP)   /*hsvHue[0] += THRESHOLD_STEP*/;
            else
                camera.setHsvHueMin(localHsvHue[1])                    /*hsvHue[0] = hsvHue[1]*/;
            gp1.dpUp.updateSnapshot(runtime);
        }

        // HUE MAXIMUM
        if (gamepad1.y && gp1.y.ready(runtime)) {
            if (localHsvHue[1] < HUE_MAX)
                camera.setHsvHueMax(localHsvHue[1] + THRESHOLD_STEP)   /*hsvHue[1] += THRESHOLD_STEP*/;
            else
                camera.setHsvHueMax(HUE_MAX)                           /*hsvHue[1] = HUE_MAX*/;
            gp1.y.updateSnapshot(runtime);
        }

        if (gamepad1.a && gp1.a.ready(runtime)) {
            if (localHsvHue[1] > localHsvHue[0])
                camera.setHsvHueMax(localHsvHue[1] - THRESHOLD_STEP)   /*hsvHue[1] -= THRESHOLD_STEP*/;
            else
                camera.setHsvHueMax(localHsvHue[0])                    /*hsvHue[1] = hsvHue[0]*/;
            gp1.a.updateSnapshot(runtime);
        }

        // SAT MINIMUM
        if (gamepad1.dpad_left && gp1.dpLeft.ready(runtime)) {
            if (localHsvSat[0] > HSV_MIN)
                camera.setHsvSatMin(localHsvSat[0] - THRESHOLD_STEP)   /*hsvSat[0] -= THRESHOLD_STEP*/;
            else
                camera.setHsvSatMin(HSV_MIN)                           /*hsvSat[0] = HSV_MIN*/;
            gp1.dpLeft.updateSnapshot(runtime);
        }

        if (gamepad1.dpad_right && gp1.dpRight.ready(runtime)) {
            if (localHsvSat[0] < localHsvSat[1])
                camera.setHsvSatMin(localHsvSat[0] + THRESHOLD_STEP)   /*hsvSat[0] += THRESHOLD_STEP*/;
            else
                camera.setHsvSatMin(localHsvSat[1])                    /*hsvSat[0] = hsvSat[1]*/;
            gp1.dpRight.updateSnapshot(runtime);
        }

        // SAT MAXIMUM
        if (gamepad1.b && gp1.b.ready(runtime)) {
            if (localHsvSat[1] < SAT_MAX)
                camera.setHsvSatMax(localHsvSat[1] + THRESHOLD_STEP)   /*hsvSat[1] += THRESHOLD_STEP*/;
            else
                camera.setHsvSatMax(SAT_MAX)                           /*hsvSat[1] = SAT_MAX*/;
            gp1.b.updateSnapshot(runtime);
        }

        if (gamepad1.x && gp1.x.ready(runtime)) {
            if (localHsvSat[1] > localHsvSat[0])
                camera.setHsvSatMax(localHsvSat[1] - THRESHOLD_STEP)   /*hsvSat[1] -= THRESHOLD_STEP*/;
            else
                camera.setHsvSatMax(localHsvSat[0])                    /*hsvSat[1] = hsvSat[0]*/;
            gp1.x.updateSnapshot(runtime);
        }

        // VAL MINIMUM
        if (gamepad1.left_trigger > TRIGGER_THRESHOLD && gp1.lt.ready(runtime)) {
            if (localHsvVal[0] > HSV_MIN)
                camera.setHsvValMin(localHsvVal[0] - THRESHOLD_STEP)   /*hsvVal[0] -= THRESHOLD_STEP*/;
            else
                camera.setHsvValMin(HSV_MIN)                           /*hsvVal[0] = HSV_MIN*/;
            gp1.lt.updateSnapshot(runtime);
        }

        if (gamepad1.left_bumper && gp1.lb.ready(runtime)) {
            if (localHsvVal[0] < localHsvVal[1])
                camera.setHsvValMin(localHsvVal[0] + THRESHOLD_STEP)   /*hsvVal[0] += THRESHOLD_STEP*/;
            else
                camera.setHsvValMin(localHsvVal[1])                    /*hsvVal[0] = hsvVal[1]*/;
            gp1.lb.updateSnapshot(runtime);
        }

        // VAL MAXIMUM
        if (gamepad1.right_trigger > TRIGGER_THRESHOLD && gp1.rt.ready(runtime)) {
            if (localHsvVal[1] > localHsvVal[0])
                camera.setHsvValMax(localHsvVal[1] - THRESHOLD_STEP)  /*hsvVal[1] -= THRESHOLD_STEP*/;
            else
                camera.setHsvValMax(localHsvVal[0])                   /*hsvVal[1] = hsvVal[0]*/;
            gp1.rt.updateSnapshot(runtime);
        }

        if (gamepad1.right_bumper && gp1.rb.ready(runtime)) {
            if (localHsvVal[1] < VAL_MAX)
                camera.setHsvValMax(localHsvVal[1] + THRESHOLD_STEP)   /*hsvVal[1] += THRESHOLD_STEP*/;
            else
                camera.setHsvValMax(VAL_MAX)                           /*hsvVal[1] = VAL_MAX*/;
            gp1.rb.updateSnapshot(runtime);
        }

        //--------------------------------------------------------------------------------------
        // END HSV THRESHOLD CONTROLS
        //--------------------------------------------------------------------------------------


            /*
                NEW Controls: (left stick and right stick configuration)
                    - Left stick: change top-left corner values relative to
                        ~ left_stick_x (changes left bound)
                        ~ left_stick_y (changes top bound)
                    - Right stick: change bottom-right corner values relative to
                        ~ right_stick_x (changes right bound)
                        ~ right_stick_y (changes bottom bound)
             */


        // Get rectangle boundaries
        double localRectTop = camera.getRectTop();
        double localRectLeft = camera.getRectLeft();
        double localRectBot = camera.getRectBot();
        double localRectRight = camera.getRectRight();

        double localBound = camera.getBound();

        camera.setRectTop(Range.clip(localRectTop + (gamepad2.left_stick_y * RECT_STEP), RECT_MIN, IMG_HEIGHT))   /*rectTop     = trim(rectTop      + (gamepad2.left_stick_y * RECT_STEP), RECT_MIN, IMG_HEIGHT)*/;
        camera.setRectLeft(Range.clip(localRectLeft + (gamepad2.left_stick_x * RECT_STEP), RECT_MIN, IMG_WIDTH))    /*rectLeft    = trim(rectLeft     + (gamepad2.left_stick_x * RECT_STEP), RECT_MIN, IMG_WIDTH)*/;
        camera.setRectBot(Range.clip(localRectBot + (gamepad2.right_stick_y * RECT_STEP), RECT_MIN, IMG_HEIGHT))  /*rectBot     = trim(rectBot      + (gamepad2.right_stick_y * RECT_STEP), RECT_MIN, IMG_HEIGHT)*/;
        camera.setRectRight(Range.clip(localRectRight + (gamepad2.right_stick_x * RECT_STEP), RECT_MIN, IMG_WIDTH))   /*rectRight   = trim(rectRight    + (gamepad2.right_stick_x * RECT_STEP), RECT_MIN, IMG_WIDTH)*/;

        // Bound is set to between left and right and controlled by dpad up and down
        if (gamepad2.dpad_left && gp2.dpLeft.ready(runtime)) {
             localBound -= RECT_STEP;
             gp2.dpLeft.updateSnapshot(runtime);
         }
         if (gamepad2.dpad_right && gp2.dpRight.ready(runtime)) {
             localBound += RECT_STEP;
             gp2.dpRight.updateSnapshot(runtime);
         }
        camera.setBound(Range.clip(localBound, localRectLeft, localRectRight)); // Updates at end for when bounding rect changes

        //camera.setBound(Range.clip(localBound + RECT_STEP * ((gamepad2.dpad_up ? 1 : 0) - (gamepad2.dpad_down ? 1 : 0)), localRectLeft, localRectRight));

        //--------------------------------------------------------------------------------------
        // START OVERLAY CONTROLS
        //--------------------------------------------------------------------------------------

            /*
                Controls:
                gp2.a   Toggle Selected Overlay
                gp2.b   Disable All Overlays
                gp2.y   Move Forward in Overlay Selection
                gp2.x   Move Backward in Overlay Selection

             */

        // Toggle current point
        if (gamepad2.a && buttonCooldown[0]) {
            overlays[togglePoint] = !overlays[togglePoint];
            buttonCooldown[0] = false;
        } else if (!gamepad2.a && !buttonCooldown[0]) buttonCooldown[0] = true;

        // Disable all overlays
        if (gamepad2.b) Arrays.fill(overlays, false);

        // Move backward in queue
        if (gamepad2.x && buttonCooldown[2]) {
            togglePoint = (--togglePoint + overlays.length) % overlays.length;
            buttonCooldown[2] = false;
        } else if (!gamepad2.x && !buttonCooldown[2]) buttonCooldown[2] = true;

        // Move forward in queue
        if (gamepad2.y && buttonCooldown[3]) {
            togglePoint = (++togglePoint) % overlays.length;
            buttonCooldown[3] = false;
        } else if (!gamepad2.y && !buttonCooldown[3]) buttonCooldown[3] = true;

        // Target Color
        camera.showTargetColor(overlays[0]);

        // Rect
        camera.setDrawRect(overlays[1]);

        // Point
        camera.setShowPoint(overlays[2]);

        // Black & White
        camera.setReturnHSV(overlays[3]);

        //--------------------------------------------------------------------------------------
        // END OVERLAY CONTROLS
        //--------------------------------------------------------------------------------------

        contours = camera.getContoursOutput();
//        double contoursLeft = 0;
//        double contoursCenter = 0;
//        double contoursRight = 0;
        double confidence = 0;

        /*
              Camera's View (cv)
        +-----------------+
        |    l      m     |   r
      y |   []      []    |   []
        |                 |
    minx+--------a--------+maxx
                    x
        split cv into 2 sections at x=a
        loop thru contours (c)
            if c.x<a
                c.addArea to l
            else
                c.addArea to m
        end
        if([l,m]=[0,0])
            run r
        else
            run max([l,m])
        end
        store past 10 entries


         */

        // Create Point variable holding top-center coordinates of boundingRect
        Point rectPoint = new Point();

        DuckPatternPipeline.rectPoints.clear();

        List<Pair<Double, Double>> xWeights = new ArrayList<>();
//        double weightedXPos = 0.0;
//        double totalWeight  = 0.0;

        if (telemetry != null) {
            telemetry.addData("contour count", contours.size());
        }
        if (multiTelemetry != null) {
            multiTelemetry.addData("Contour Count", contours.size());
        }
        try {
            for (MatOfPoint c : contours) {
                Rect contourRect = Imgproc.boundingRect(c);

                // Telemetry data on contour location
                //                telemetry.addData("contour x", contourRect.x);
                //                telemetry.addData("contour y", contourRect.y);
                //                telemetry.addData("contour w", contourRect.width);
                //                telemetry.addData("contour h", contourRect.height);

                // Ensure contour is inside bounding box
                if (contourRect.x >= localRectLeft &&
                        contourRect.y >= localRectTop &&
                        contourRect.x + contourRect.width <= localRectRight &&
                        contourRect.y + contourRect.height <= localRectBot) {


                    // Get the center of the contour
                    rectPoint.x = contourRect.width/2.0 + contourRect.x;
                    rectPoint.y = contourRect.y + contourRect.height/2.0;

                    Pair<Double, Double> weight = new Pair<>(rectPoint.x, contourRect.area());
                    xWeights.add(weight);
//                    weightedXPos += rectPoint.x * contourRect.area();
//                    totalWeight += contourRect.area();


                    DuckPatternPipeline.rectPoints.add(new Point(rectPoint.x, rectPoint.y));

                    if (telemetry != null && !stopPointTelemetry) {
                        telemetry.addLine("In box");
                        telemetry.addData("rect point", new Point(rectPoint.x, rectPoint.y));
                        telemetry.addLine();
                    }
                    if (multiTelemetry != null && !stopPointTelemetry) {
                        multiTelemetry.addLine("In box");
                        multiTelemetry.addData("Rect point", new Point(rectPoint.x, rectPoint.y));
                        multiTelemetry.addLine();
                    }

                    // Check which region contour is in
//                    if (rectPoint.x < localBound) {
//                        contoursLeft += contourRect.area();
//                    } else {
//                        contoursCenter += contourRect.area();
//                    }

                }

            }
        } catch (Exception e) {
            if (telemetry != null) {
                telemetry.addLine("Error while iterating through contours!");
            }
            if (multiTelemetry != null) {
                multiTelemetry.addLine("Error while iterating through contours!");
            }

            //contourIterateError = true;
        }

         DuckPlacement currentPlacement = DuckPlacement.RIGHT;


         if(xWeights.size() != 0) {
            double sumXWeight = xWeights.stream().mapToDouble(pair -> pair.fst * pair.snd).sum();
            double totalWeight = xWeights.stream().mapToDouble(pair -> pair.snd).sum();
            double weightedXPos = sumXWeight / totalWeight;
            DuckPatternPipeline.rectPoints.add(new Point(weightedXPos, IMG_HEIGHT * 0.75));

            for(int i = 0; i < IMG_HEIGHT / 4; i += 20) {
                DuckPatternPipeline.rectPoints.add(new Point(IMG_WIDTH * Bound1Percent / 100.0, IMG_HEIGHT - i));
                DuckPatternPipeline.rectPoints.add(new Point(IMG_WIDTH * Bound2Percent / 100.0, IMG_HEIGHT - i));
//                DuckPatternPipeline.rectPoints.add(new Point(IMG_WIDTH * 0.6, IMG_WIDTH - i));
                DuckPatternPipeline.rectPoints.add(new Point(IMG_WIDTH * Bound3Percent / 100.0, IMG_HEIGHT - i));
            }

             confidence = Math.sqrt(totalWeight / 20000.0);

            currentPlacement = PlacementByBounds(weightedXPos);

//            if(weightedXPos < IMG_WIDTH * 0.15) {
//                currentPlacement = DuckPlacement.LEFT;
//                confidence *= 1.1; // Slight multiplier due to small possible range of values
////                telemetry.addLine("LEFT");
//            } else if(weightedXPos < IMG_WIDTH * 0.5469) {
//                currentPlacement = DuckPlacement.CENTER;
//
////                telemetry.addLine("CENTER");
//            } else if(weightedXPos < IMG_WIDTH * 0.95) {
//                currentPlacement = DuckPlacement.RIGHT;
//
////                telemetry.addLine("RIGHT");
//            }

            confidence = Math.min(confidence, 1.0);
//            confidence = x;



            if(telemetry != null) {
                telemetry.addData("Placement:", currentPlacement);
                telemetry.addData("Weighted x pos:", sumXWeight);
                telemetry.addData("Total weight:", totalWeight);
                telemetry.addData("Ratio \" \"", weightedXPos);
                telemetry.addLine();
            }
        } else {
             currentPlacement = DuckPlacement.RIGHT;
             confidence = 0.825;
         }


//        if(contoursLeft != 0 || contoursCenter != 0) { // L & C not 0
//            if(contoursLeft > contoursCenter) {
//                currentPlacement = DuckPlacement.LEFT;
//            } else {
//                currentPlacement = DuckPlacement.RIGHT;
//            }
//            confidence = Math.abs(contoursLeft - contoursCenter) / (contoursLeft + contoursCenter);
//        } else {
//            confidence = 0.80; // 80% confidence if we don't see anything
//        }

        placement = currentPlacement;
        Pair<DuckPlacement, Double> placementDataEntry = new Pair<>(currentPlacement, confidence);
        fullPlacementData.add(placementDataEntry);
        if (fullPlacementData.size() > maxPlacementDataSize)
            fullPlacementData.remove(0);

        //
        //
        //
        //        // Get the largest tally
        //        double largestTally = largest(contoursProportionLeft, contoursProportionCenter, contoursProportionRight);
        //
        //        // Divide all three tallies by the largest to get proportions
        //        try {
        //            contoursProportionLeft /= largestTally;
        //            contoursProportionCenter /= largestTally;
        //            contoursProportionRight /= largestTally;
        //        } catch (Exception e) {
        //            telemetry.addLine("Error while dividing contour tallies by largest tally.");
        //        }
        //
        //        // Get the smallest proportioned tally
        //        double smallestProportionedTally = smallest(contoursProportionLeft, contoursProportionCenter, contoursProportionRight);
        //
        //        // Subtract from one to get confidence
        //        double confidence = 1.0 - smallestProportionedTally;
        //
        //
        //        // Compare area tallies before determining badData to take advantage of SkystonePlacement.UNKNOWN
        //        RingPlacement currentPlacement =
        //                compareAreaTallies(contoursProportionLeft, contoursProportionCenter, contoursProportionRight);


        // Compare contour area tallies to see which third of the bounding rectangle
        // has the least (which will be the third with the Skystone in it).
        // If data is below our confidence threshold, keep the last reading instead
        // of getting a new one from bad data.
        //        boolean badData =
        //                confidence < SKYSTONE_CONFIDENCE_THRESHOLD ||
        //                        Double.isNaN(confidence) ||
        //                        contourIterateError ||
        //                        currentPlacement == UNKNOWN;    // true if confidence is too low or if we get NaN as confidence or if contour iteration fails
        //        if (badData) {
        //            // Do nothing; last reading will be kept
        //        } else {
        //            // Good data! Update our decision.
        //            placement = currentPlacement;
        //        }

        camera.periodic();

        if (telemetry != null) {
            telemetry.addLine("Running!");
            telemetry.addLine();
            telemetry.addLine(String.format(Locale.ENGLISH, "Hue: [%.2f, %.2f]", localHsvHue[0], localHsvHue[1]));
            telemetry.addLine(String.format(Locale.ENGLISH, "Sat: [%.2f, %.2f]", localHsvSat[0], localHsvSat[1]));
            telemetry.addLine(String.format(Locale.ENGLISH, "Val: [%.2f, %.2f]", localHsvVal[0], localHsvVal[1]));
            telemetry.addLine();
            telemetry.addLine(String.format(Locale.ENGLISH, "Hue\': [%.2f, %.2f]", localHsvHue[0]*2, localHsvHue[1]*2));
            telemetry.addLine(String.format(Locale.ENGLISH, "Sat\': [%.2f, %.2f]", localHsvSat[0]*0.39215686274, localHsvSat[1]*0.39215686274));
            telemetry.addLine(String.format(Locale.ENGLISH, "Val\': [%.2f, %.2f]", localHsvVal[0]*0.39215686274, localHsvVal[1]*0.39215686274));
            telemetry.addLine();
//            telemetry.addData("contoursLeft", String.format(Locale.ENGLISH, "%.2f", contoursLeft));
//            telemetry.addData("contoursCenter", String.format(Locale.ENGLISH, "%.2f", contoursCenter));
//            telemetry.addData("contoursRight", String.format(Locale.ENGLISH, "%.2f", contoursRight));
            telemetry.addLine();
            telemetry.addData("placement", placement);
            telemetry.addData("confidence", confidence);
            telemetry.addData("adjustedPlacement", calculateAdjustedPlacement());
            telemetry.addLine();
            telemetry.addData("Rect left", String.format(Locale.ENGLISH, "%.2f", localRectLeft));
            telemetry.addData("Rect top", String.format(Locale.ENGLISH, "%.2f", localRectTop));
            telemetry.addData("Rect right", String.format(Locale.ENGLISH, "%.2f", localRectRight));
            telemetry.addData("Rect bottom", String.format(Locale.ENGLISH, "%.2f", localRectBot));
            telemetry.addLine();
            telemetry.addData("bound", String.format(Locale.ENGLISH, "%.2f", camera.getBound()));
            telemetry.addLine();
            telemetry.addLine("Visual Modifiers");
            telemetry.addData((togglePoint==0?"*":"")+"Show Target Color", overlays[0]);
            telemetry.addData((togglePoint==1?"*":"")+"Show Rect", overlays[1]);
            telemetry.addData((togglePoint==2?"*":"")+"Show Point", overlays[2]);
            telemetry.addData((togglePoint==3?"*":"")+"Show HSV", overlays[3]);
            //            telemetry.addLine();
            //            telemetry.addData("Confidence", String.format(Locale.ENGLISH, "%.2f", confidence));
            //            if (badData)
            //                telemetry.addLine("Confidence is below threshold or not a number. Keeping last placement decision.");
            telemetry.update();
        }
        if (multiTelemetry != null) {
            multiTelemetry.addLine("Running!");
            multiTelemetry.addLine();
            multiTelemetry.addLine(String.format(Locale.ENGLISH, "Hue: [%.2f, %.2f]", localHsvHue[0]*2, localHsvHue[1]*2));
            multiTelemetry.addLine(String.format(Locale.ENGLISH, "Sat: [%.2f, %.2f]", localHsvSat[0]*0.39215686274, localHsvSat[1]*0.39215686274));
            multiTelemetry.addLine(String.format(Locale.ENGLISH, "Val: [%.2f, %.2f]", localHsvVal[0]*0.39215686274, localHsvVal[1]*0.39215686274));
            multiTelemetry.addLine();
//            multiTelemetry.addData("contoursLeft", String.format(Locale.ENGLISH, "%.2f", contoursLeft));
//            multiTelemetry.addData("contoursCenter", String.format(Locale.ENGLISH, "%.2f", contoursCenter));
//            multiTelemetry.addData("contoursRight", String.format(Locale.ENGLISH, "%.2f", contoursRight));
            multiTelemetry.addLine();
            multiTelemetry.addData("placement", placement);
            multiTelemetry.addData("confidence", confidence);
            multiTelemetry.addLine();
            multiTelemetry.addData("Rect left", String.format(Locale.ENGLISH, "%.2f", localRectLeft));
            multiTelemetry.addData("Rect top", String.format(Locale.ENGLISH, "%.2f", localRectTop));
            multiTelemetry.addData("Rect right", String.format(Locale.ENGLISH, "%.2f", localRectRight));
            multiTelemetry.addData("Rect bottom", String.format(Locale.ENGLISH, "%.2f", localRectBot));
            multiTelemetry.addLine();
            multiTelemetry.addData("bound", String.format(Locale.ENGLISH, "%.2f", camera.getBound()));
            multiTelemetry.addLine();
            multiTelemetry.addLine("Visual Modifiers");
            multiTelemetry.addData("Show Target Color", overlays[0]);
            multiTelemetry.addData("Show Rect", overlays[1]);
            multiTelemetry.addData("Show Point", overlays[2]);
            multiTelemetry.addData("Show HSV", overlays[3]);
//            telemetry.addLine();
//            telemetry.addData("Confidence", String.format(Locale.ENGLISH, "%.2f", confidence));
//            if (badData)
//                telemetry.addLine("Confidence is below threshold or not a number. Keeping last placement decision.");
            multiTelemetry.update();
        }
    }


    public double largest(double ...arr){
        return Math.max(arr[0],Math.max(arr[1],arr[2]));
    }

    public double smallest(double ...arr) {
        return Math.min(arr[0],Math.min(arr[1],arr[2]));
    }

    public DuckPlacement getPlacement() {
        return placement;
    }
    public ArrayList<Pair<DuckPlacement, Double>> getFullPlacementData() {
        return fullPlacementData;
    }

    private void normalizeByMagnitude(double[] arr) {
        double maxMagnitude = Math.abs(arr[0]);
        for (int i = 1; i < arr.length; i++) {
            double temp = Math.abs(arr[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        if (maxMagnitude > 1.0) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] /= maxMagnitude;
            }
        }
    }

    /**
     * Returns population variance of a double array
     *
     * @param arr Array to solve variance
     * @return The array's variance
     */
    private double getVariance(double[] arr) {
        // var(x) = sum((mu-x)^2)/n = (mu^2+sum(x^2)/n)/n
        double mu1 = 0;
        double mu2 = 0;
        double len = arr.length;
        for(double d: arr) {
            mu1 += d;
            mu2 += d*d;
        }


        return (mu1*mu1 + mu2/len)/len;
    }

    private Pair<DuckPlacement, Double> calculateAdjustedPlacement() {
        Pair<DuckPlacement, Double> newPlacement = new Pair<>(DuckPlacement.UNKNOWN, 0.0);
        double[] confSums = {0.0, 0.0, 0.0, 0,0};
        double maxPossible = 0.0;

        for(int i = 0; i < fullPlacementData.size(); i++) {
            Pair<DuckPlacement, Double> data = fullPlacementData.get(i);
            double confAdj = data.snd * Math.sqrt(1-0.75*i/fullPlacementData.size()); // Decay formula = sqrt(1-(1-min^2)*i)/n) where min = 1/2
            switch (data.fst) {
                case LEFT:
                    confSums[0] += confAdj;
                    break;
                case CENTER:
                    confSums[1] += confAdj;
                    break;
                case RIGHT:
                    confSums[2] += confAdj;
                    break;
                case UNKNOWN:
                    confSums[3] += confAdj * 0.9;// Scale UNKNOWN by even more to reduce it's occurrence
            }

            maxPossible += Math.sqrt(1-0.75*i/fullPlacementData.size()); // Scale to [0,1]
        }

        double maxConf = largest(confSums);
        if(confSums[0] == maxConf) {
            newPlacement = new Pair<>(DuckPlacement.LEFT, util.roundTo(confSums[0] / maxPossible, 4));
        }
        if(confSums[1] == maxConf) {
            newPlacement = new Pair<>(DuckPlacement.CENTER, util.roundTo(confSums[1] / maxPossible, 4));
        }
        if(confSums[2] == maxConf) {
            newPlacement = new Pair<>(DuckPlacement.RIGHT, util.roundTo(confSums[2] / maxPossible, 4));
        }
        if(confSums[3] > maxConf) {
            newPlacement = new Pair<>(DuckPlacement.UNKNOWN, util.roundTo(confSums[3] / maxPossible, 4));
        }


        return newPlacement;
    }

    private DuckPlacement PlacementByBounds(double pos) {
        if(pos < IMG_WIDTH * Bound1Percent / 100.0) {
            return DuckPlacement.LEFT;
//            confidence *= 1.1; // Slight multiplier due to small possible range of values
//                telemetry.addLine("LEFT");
        } else if(pos < IMG_WIDTH * Bound2Percent / 100.0) {
            return DuckPlacement.CENTER;

//                telemetry.addLine("CENTER");
        } else if(pos < IMG_WIDTH * Bound3Percent / 100.0) {
            return DuckPlacement.RIGHT;

//                telemetry.addLine("RIGHT");
        }
        return DuckPlacement.RIGHT;
    }

    public Camera getCamera() {
        return camera;
    }
}
