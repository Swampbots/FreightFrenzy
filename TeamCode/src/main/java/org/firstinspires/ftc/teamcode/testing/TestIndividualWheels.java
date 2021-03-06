package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.subsystems.Drive;

@Disabled
@TeleOp(name = "test each wheel", group = "testing")
public class TestIndividualWheels extends OpMode {
    Drive drive;
    private final double POWER_SCALE = 0.5;

    @Override
    public void init() {
        drive = new Drive(hardwareMap, true);
        drive.initHardware();

        telemetry.addLine("Ready!");
        telemetry.update();
    }

    @Override
    public void loop() {
        double fl = gamepad1.a ? 1 : 0; //rl
        double fr = gamepad1.b ? 1 : 0; //rr
        double rl = gamepad1.x ? 1 : 0; //fl
        double rr = gamepad1.y ? 1 : 0; //fr

        drive.setPower(fl * POWER_SCALE, fr*POWER_SCALE, rl*POWER_SCALE, rr*POWER_SCALE);
        if(fl == 0 && fr == 0 && rl == 0 && rr == 0) {
//            drive.setPower(gamepad1.left_stick_y, gamepad1.right_stick_y);
            drive.setRocketLeaguePower(gamepad1.right_trigger - gamepad1.left_trigger, gamepad1.left_stick_x * 0.75, false);
        }
        drive.periodic();

        telemetry.addData("fl", fl);
        telemetry.addData("fr", fr);
        telemetry.addData("rl", rl);
        telemetry.addData("rr", rr);
        telemetry.update();
    }
}
