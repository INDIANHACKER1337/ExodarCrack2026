/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

import java.awt.Color;

/**
 * ColorSetting - A single setting that stores a color (R, G, B, A)
 * Displayed as a hue slider with color preview in the GUI
 */
public class ColorSetting extends Setting {
    private int red;
    private int green;
    private int blue;
    private int alpha;
    private final int defaultRed;
    private final int defaultGreen;
    private final int defaultBlue;
    private final int defaultAlpha;
    private final boolean hasAlpha;

    // For rainbow mode
    private boolean rainbow;
    private float rainbowSpeed = 1.0f;
    private float rainbowOffset = 0;

    public ColorSetting(String name, int red, int green, int blue) {
        this(name, red, green, blue, 255, false);
    }

    public ColorSetting(String name, int red, int green, int blue, int alpha) {
        this(name, red, green, blue, alpha, true);
    }

    public ColorSetting(String name, int red, int green, int blue, int alpha, boolean hasAlpha) {
        super(name);
        this.red = clamp(red, 0, 255);
        this.green = clamp(green, 0, 255);
        this.blue = clamp(blue, 0, 255);
        this.alpha = clamp(alpha, 0, 255);
        this.defaultRed = this.red;
        this.defaultGreen = this.green;
        this.defaultBlue = this.blue;
        this.defaultAlpha = this.alpha;
        this.hasAlpha = hasAlpha;
        this.rainbow = false;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setRed(int red) {
        this.red = clamp(red, 0, 255);
    }

    public void setGreen(int green) {
        this.green = clamp(green, 0, 255);
    }

    public void setBlue(int blue) {
        this.blue = clamp(blue, 0, 255);
    }

    public void setAlpha(int alpha) {
        this.alpha = clamp(alpha, 0, 255);
    }

    public void setColor(int red, int green, int blue) {
        this.red = clamp(red, 0, 255);
        this.green = clamp(green, 0, 255);
        this.blue = clamp(blue, 0, 255);
    }

    public void setColor(int red, int green, int blue, int alpha) {
        setColor(red, green, blue);
        this.alpha = clamp(alpha, 0, 255);
    }

    /**
     * Get color as ARGB int
     */
    public int getColor() {
        if (rainbow) {
            return getRainbowColor();
        }
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Get color as RGB int (no alpha)
     */
    public int getRGB() {
        if (rainbow) {
            return getRainbowColor() & 0x00FFFFFF;
        }
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Get color as java.awt.Color
     */
    public Color getAwtColor() {
        if (rainbow) {
            int c = getRainbowColor();
            return new Color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, alpha);
        }
        return new Color(red, green, blue, alpha);
    }

    /**
     * Get hue value (0-360)
     */
    public float getHue() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[0] * 360;
    }

    /**
     * Get saturation (0-1)
     */
    public float getSaturation() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[1];
    }

    /**
     * Get brightness (0-1)
     */
    public float getBrightness() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[2];
    }

    /**
     * Set color from HSB values
     */
    public void setFromHSB(float hue, float saturation, float brightness) {
        int rgb = Color.HSBtoRGB(hue / 360f, saturation, brightness);
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
    }

    /**
     * Set hue while keeping saturation and brightness
     */
    public void setHue(float hue) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hue, hsb[1], hsb[2]);
    }

    /**
     * Set saturation while keeping hue and brightness
     */
    public void setSaturation(float saturation) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hsb[0] * 360, saturation, hsb[2]);
    }

    /**
     * Set brightness while keeping hue and saturation
     */
    public void setBrightness(float brightness) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hsb[0] * 360, hsb[1], brightness);
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public void toggleRainbow() {
        this.rainbow = !this.rainbow;
    }

    public float getRainbowSpeed() {
        return rainbowSpeed;
    }

    public void setRainbowSpeed(float speed) {
        this.rainbowSpeed = speed;
    }

    public float getRainbowOffset() {
        return rainbowOffset;
    }

    public void setRainbowOffset(float offset) {
        this.rainbowOffset = offset;
    }

    private int getRainbowColor() {
        float hue = (System.currentTimeMillis() % (long)(10000 / rainbowSpeed)) / (10000f / rainbowSpeed);
        hue = (hue + rainbowOffset) % 1.0f;
        int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    @Override
    public void resetToDefault() {
        this.red = defaultRed;
        this.green = defaultGreen;
        this.blue = defaultBlue;
        this.alpha = defaultAlpha;
        this.rainbow = false;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Get hex string representation
     */
    public String getHexString() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    /**
     * Set from hex string
     */
    public void setFromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() >= 6) {
            try {
                this.red = Integer.parseInt(hex.substring(0, 2), 16);
                this.green = Integer.parseInt(hex.substring(2, 4), 16);
                this.blue = Integer.parseInt(hex.substring(4, 6), 16);
            } catch (NumberFormatException ignored) {}
        }
    }
}
