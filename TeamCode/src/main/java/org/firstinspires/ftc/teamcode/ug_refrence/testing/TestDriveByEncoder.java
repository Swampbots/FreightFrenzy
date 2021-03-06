package org.firstinspires.ftc.teamcode.ug_refrence.testing;

import com.disnodeteam.dogecommander.DogeCommander;
import com.disnodeteam.dogecommander.DogeOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.ug_refrence.robot.commands.auto.DriveByEncoder;
import org.firstinspires.ftc.teamcode.ug_refrence.robot.subsystems.Drive;

@Disabled
public class TestDriveByEncoder  extends LinearOpMode implements DogeOpMode {
    //private DcMotor shooterMotor;
    @Override
    public void runOpMode() throws InterruptedException {
        DogeCommander commander = new DogeCommander(this);

        Drive drive = new Drive(hardwareMap, true);
        //Kicker kicker = new Kicker(hardwareMap);
        //Shooter shooter         = new Shooter(hardwareMap);
        //Arm arm = new Arm(hardwareMap);
        //Grip grip = new Grip(hardwareMap);
        //Intake intake = new Intake(hardwareMap);
        //Transfer transfer = new Transfer(hardwareMap);

        commander.registerSubsystem(drive);
        //commander.registerSubsystem(kicker);
        //commander.registerSubsystem(shooter);
        //commander.registerSubsystem(arm);
        //commander.registerSubsystem(grip);
        //commander.registerSubsystem(intake);
        //commander.registerSubsystem(transfer);

        //shooterMotor = hardwareMap.get(DcMotor.class, "shooter");

        commander.init();

        waitForStart();

        telemetry.addData("Fl0",drive.getCurrentPositions()[0]);
        telemetry.addData("Fr0",drive.getCurrentPositions()[1]);
        telemetry.addData("Rl0",drive.getCurrentPositions()[2]);
        telemetry.addData("Rr0",drive.getCurrentPositions()[3]);
        telemetry.update();

        telemetry.addData("Fl0",drive.getCurrentPositions()[0]);
        telemetry.addData("Fr0",drive.getCurrentPositions()[1]);
        telemetry.addData("Rl0",drive.getCurrentPositions()[2]);
        telemetry.addData("Rr0",drive.getCurrentPositions()[3]);
        telemetry.addLine();
        commander.runCommand(new DriveByEncoder(drive,(int)(Drive.COUNTS_PER_INCH_EMPIRICAL*5),0,0.3,5));

        telemetry.addData("Fl0",drive.getCurrentPositions()[0]);
        telemetry.addData("Fr0",drive.getCurrentPositions()[1]);
        telemetry.addData("Rl0",drive.getCurrentPositions()[2]);
        telemetry.addData("Rr0",drive.getCurrentPositions()[3]);
        telemetry.update();
//

        sleep(20000);
        commander.stop();
    }
}
