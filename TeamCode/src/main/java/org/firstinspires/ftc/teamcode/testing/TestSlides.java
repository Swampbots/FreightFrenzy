package org.firstinspires.ftc.teamcode.testing;

import com.disnodeteam.dogecommander.DogeCommander;
import com.disnodeteam.dogecommander.DogeOpMode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.robot.commands.teleop.TeleOpSlideControl;
import org.firstinspires.ftc.teamcode.robot.subsystems.Slides;

public class TestSlides extends LinearOpMode implements DogeOpMode {

    @Override
    public void runOpMode() throws InterruptedException {
        DogeCommander commander = new DogeCommander(this);

        Slides slides = new Slides(hardwareMap);

        commander.registerSubsystem(slides);

        commander.init();

        waitForStart();

        commander.runCommandsParallel(new TeleOpSlideControl(slides, gamepad1, telemetry));
    }
}