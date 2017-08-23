package com.duy.screenfilter.model;

import android.graphics.Color;

import java.io.Serializable;

/**
 * Created by Duy on 23-Aug-17.
 */

public class ColorProfile implements Serializable {
    private static final float DIM_MAX_ALPHA = 0.9f;
    private static final float INTENSITY_MAX_ALPHA = 0.75f;
    private static final float ALPHA_ADD_MULTIPLIER = 0.75f;

    private int color;
    private int intensity;
    private int dimLevel;
    private boolean lowerBrightness;

    public ColorProfile(int color, int intensity, int dimLevel, boolean lowerBrightness) {
        this.color = color;
        this.intensity = intensity;
        this.dimLevel = dimLevel;
        this.lowerBrightness = lowerBrightness;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getDimLevel() {
        return dimLevel;
    }

    public void setDimLevel(int dimLevel) {
        this.dimLevel = dimLevel;
    }

    public boolean isLowerBrightness() {
        return lowerBrightness;
    }

    public void setLowerBrightness(boolean lowerBrightness) {
        this.lowerBrightness = lowerBrightness;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Profile{");
        sb.append("color=").append(color);
        sb.append(", intensity=").append(intensity);
        sb.append(", dimLevel=").append(dimLevel);
        sb.append(", lowerBrightness=").append(lowerBrightness);
        sb.append('}');
        return sb.toString();
    }

    public int getFilterColor() {
        int rgbColor = rgbFromColor(color);
        int intensityColor = Color.argb(floatToColorBits(intensity / 100.0f),
                Color.red(rgbColor),
                Color.green(rgbColor),
                Color.blue(rgbColor));
        int dimColor = Color.argb(floatToColorBits(dimLevel / 100.0f), 0, 0, 0);
        return addColors(dimColor, intensityColor);
    }

    private int addColors(int color1, int color2) {
        float alpha1 = colorBitsToFloat(Color.alpha(color1));
        float alpha2 = colorBitsToFloat(Color.alpha(color2));
        float red1 = colorBitsToFloat(Color.red(color1));
        float red2 = colorBitsToFloat(Color.red(color2));
        float green1 = colorBitsToFloat(Color.green(color1));
        float green2 = colorBitsToFloat(Color.green(color2));
        float blue1 = colorBitsToFloat(Color.blue(color1));
        float blue2 = colorBitsToFloat(Color.blue(color2));

        // See: http://stackoverflow.com/a/10782314

        // Alpha changed to allow more control
        float fAlpha = alpha2 * INTENSITY_MAX_ALPHA + (DIM_MAX_ALPHA - alpha2 * INTENSITY_MAX_ALPHA) * alpha1;
        alpha1 *= ALPHA_ADD_MULTIPLIER;
        alpha2 *= ALPHA_ADD_MULTIPLIER;
        int alpha = floatToColorBits(fAlpha);
        int red = floatToColorBits((red1 * alpha1 + red2 * alpha2 * (1.0f - alpha1)) / fAlpha);
        int green = floatToColorBits((green1 * alpha1 + green2 * alpha2 * (1.0f - alpha1)) / fAlpha);
        int blue = floatToColorBits((blue1 * alpha1 + blue2 * alpha2 * (1.0f - alpha1)) / fAlpha);
        return Color.argb(alpha, red, green, blue);
    }

    private int floatToColorBits(float color) {
        return (int) (color * 255.0F);
    }


    private float colorBitsToFloat(int bits) {
        return (float) bits / 255.0F;
    }

    private double constrains(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // After: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
    private int rgbFromColor(int color) {
        int colorTemperature = getColorTemperature(color);
        int alpha = 255; //max
        double temp = (float) colorTemperature / 100.0f;
        int red = (int) ((temp <= 66) ? 255.0d : 329.698727446 * Math.pow(temp - 60, -0.1332047592));
        int green = (int) ((temp <= 66)
                ? 99.4708025861 * Math.log(temp) - 161.1195681661
                : 288.1221695283 * Math.pow(temp - 60, -0.0755148492));
        int blue = 0;
        if (temp >= 66) blue = (int) 255.0d;
        else if (temp > 19) blue = (int) 0.0d;
        else blue = (int) (138.5177312231 * Math.log(temp - 10) - 305.0447927307);
        return Color.argb(alpha, red, green, blue);
    }

    private int getColorTemperature(int color) {
        return 500 + color * 30;
    }
}
