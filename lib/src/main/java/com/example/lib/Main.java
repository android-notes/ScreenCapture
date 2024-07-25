package com.example.lib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.view.Display;
import android.view.SurfaceControl;
import android.window.ScreenCapture;

import java.io.FileOutputStream;
import java.io.OutputStream;

import android.view.WindowManagerGlobal;
import android.window.ScreenCapture.SynchronousScreenCaptureListener;


public class Main {
    static {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                System.err.println(throwable);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Bitmap bitmap = takeScreenshot();
        OutputStream outputStream = new FileOutputStream("/sdcard/1.png");
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
        outputStream.close();
    }

    static Bitmap takeScreenshot() {
        final int SDK_INT = SystemProperties.getInt(
                "ro.build.version.sdk", 0);

        if (SDK_INT >= 34) {
            // Take the screenshot
            final SynchronousScreenCaptureListener syncScreenCapture =
                    ScreenCapture.createSyncCaptureListener();
            try {
                WindowManagerGlobal.getWindowManagerService().captureDisplay(Display.DEFAULT_DISPLAY, null,
                        syncScreenCapture);
            } catch (RemoteException e) {
                System.err.println("Failed to take fullscreen screenshot" + e);
            }
            final ScreenCapture.ScreenshotHardwareBuffer screenshotBuffer = syncScreenCapture.getBuffer();
            final Bitmap screenShot = screenshotBuffer == null ? null : screenshotBuffer.asBitmap();
            if (screenShot == null) {
                System.err.println("Failed to take fullscreen screenshot");
                return null;
            }
            // Optimization
            screenShot.setHasAlpha(false);
            return screenShot;
        } else if (SDK_INT >= 31) {
            // Take the screenshot
            final IBinder displayToken = SurfaceControl.getInternalDisplayToken();
            final SurfaceControl.DisplayCaptureArgs captureArgs =
                    new SurfaceControl.DisplayCaptureArgs.Builder(displayToken)
                            .build();
            final SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer =
                    SurfaceControl.captureDisplay(captureArgs);
            final Bitmap screenShot = screenshotBuffer == null ? null : screenshotBuffer.asBitmap();
            if (screenShot == null) {
                System.err.println("Failed to take fullscreen screenshot");
                return null;
            }

            // Optimization
            screenShot.setHasAlpha(false);

            return screenShot;
        } else if (SDK_INT >= 28) {
            Display display = DisplayManagerGlobal.getInstance()
                    .getRealDisplay(Display.DEFAULT_DISPLAY);
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            final int displayWidth = displaySize.x;
            final int displayHeight = displaySize.y;

            int rotation = display.getRotation();
            Rect crop = new Rect(0, 0, displayWidth, displayHeight);
            System.err.println("Taking screenshot of dimensions " + displayWidth + " x " + displayHeight);
            // Take the screenshot
            Bitmap screenShot =
                    SurfaceControl.screenshot(crop, displayWidth, displayHeight, rotation);
            if (screenShot == null) {
                System.err.println("Failed to take screenshot of dimensions " + displayWidth + " x "
                        + displayHeight);
                return null;
            }

            // Optimization
            screenShot.setHasAlpha(false);

            return screenShot;
        } else {
            Display display = DisplayManagerGlobal.getInstance()
                    .getRealDisplay(Display.DEFAULT_DISPLAY);
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            final int displayWidth = displaySize.x;
            final int displayHeight = displaySize.y;

            final float screenshotWidth;
            final float screenshotHeight;

            final int rotation = display.getRotation();

            switch (rotation) {
                case 0:
                case 2: {
                    screenshotWidth = displayWidth;
                    screenshotHeight = displayHeight;
                }
                break;
                case 1:
                case 3: {
                    screenshotWidth = displayHeight;
                    screenshotHeight = displayWidth;
                }
                break;
                default: {
                    throw new IllegalArgumentException("Invalid rotation: "
                            + rotation);
                }
            }

            System.err.println("Taking screenshot of dimensions " + displayWidth + " x " + displayHeight);
            // Take the screenshot
            Bitmap screenShot =
                    SurfaceControl.screenshot((int) screenshotWidth, (int) screenshotHeight);
            if (screenShot == null) {
                System.err.println("Failed to take screenshot of dimensions " + screenshotWidth + " x "
                        + screenshotHeight);
                return null;
            }

            // Rotate the screenshot to the current orientation
            if (rotation != 0) {
                Bitmap unrotatedScreenShot = Bitmap.createBitmap(displayWidth, displayHeight,
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(unrotatedScreenShot);
                canvas.translate(unrotatedScreenShot.getWidth() / 2,
                        unrotatedScreenShot.getHeight() / 2);
                canvas.rotate(getDegreesForRotation(rotation));
                canvas.translate(-screenshotWidth / 2, -screenshotHeight / 2);
                canvas.drawBitmap(screenShot, 0, 0, null);
                canvas.setBitmap(null);
                screenShot.recycle();
                screenShot = unrotatedScreenShot;
            }

            // Optimization
            screenShot.setHasAlpha(false);

            return screenShot;
        }
    }

    private static float getDegreesForRotation(int value) {
        final int ROTATION_90 = 1;
        final int ROTATION_180 = 2;
        final int ROTATION_270 = 3;
        switch (value) {
            case ROTATION_90: {
                return 360f - 90f;
            }
            case ROTATION_180: {
                return 360f - 180f;
            }
            case ROTATION_270: {
                return 360f - 270f;
            }
            default: {
                return 0;
            }
        }
    }
}
