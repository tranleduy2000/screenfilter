package com.duy.screenfilter.model;

import android.graphics.Color;

import java.io.Serializable;

/**
 * Created by Duy on 23-Aug-17.
 */

public class ColorProfile implements Serializable, Cloneable {
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

    private int truncate(double value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return (int) value;
    }

    // After: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
    public final int rgbFromColor(int color) {
        int colorTemperature = getColorTemperature(color);
        short alpha = 255;
        double temp = (double) colorTemperature / (double) 100.0F;
        double red = temp <= (double) 66
                ? 255.0D
                : 329.698727446D * Math.pow(temp - (double) 60, -0.1332047592D);
        double green = temp <= (double) 66
                ? 99.4708025861D * Math.log(temp) - 161.1195681661D
                : 288.1221695283D * Math.pow(temp - (double) 60, -0.0755148492D);
        double blue = temp >= (double) 66 ? 255.0D : (temp < (double) 19
                ? 0.0D
                : 138.5177312231D * Math.log(temp - (double) 10) - 305.0447927307D);
        return Color.argb(alpha, truncate(red), truncate(green), truncate(blue));
    }


    private int getColorTemperature(int color) {
        return 500 + color * 30;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorProfile that = (ColorProfile) o;

        if (color != that.color) return false;
        if (intensity != that.intensity) return false;
        if (dimLevel != that.dimLevel) return false;
        return lowerBrightness == that.lowerBrightness;

    }

    @Override
    public int hashCode() {
        int result = color;
        result = 31 * result + intensity;
        result = 31 * result + dimLevel;
        result = 31 * result + (lowerBrightness ? 1 : 0);
        return result;
    }
}
