package org.firstinspires.ftc.teamcode.robot.commands.auto;

import com.disnodeteam.dogecommander.Command;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.robot.subsystems.Drive;

public class DriveByTimer implements Command {
    private Drive drive;

    private ElapsedTime timer;
    private double seconds;
    private double power;
    private Telemetry telemetry;

    public DriveByTimer(Drive drive, double seconds, double power, Telemetry telemetry){
        timer = new ElapsedTime();

        this.drive = drive;
        this.seconds = seconds;
        this.power = power;
        this.telemetry = telemetry;
    }

    public DriveByTimer(Drive drive, double seconds, double power){
        this(drive,seconds,power, null);
    }

    @Override
    public void start(){
        timer.reset();
        drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        drive.setPower(power,power);
    }

    @Override
    public void periodic() {
        if(telemetry != null) {
            telemetry.addData("Timer", timer.seconds());
            telemetry.addData("Seconds", seconds);
            telemetry.addData("encoder[0]", drive.getCurrentPositions()[0]);

            telemetry.update();
        }
    }

    @Override
    public void stop(){
        drive.setPower(0,0);
    }

    @Override
    public boolean isCompleted() {
        return timer.seconds() > seconds;
    }
}
