package cm.joel.utils;

public class HeadConfiguration {
    private final double probability;
    private final String texture;
    private final String headName;

    public HeadConfiguration(double probability, String texture, String headName) {
        this.probability = probability;
        this.texture = texture;
        this.headName = headName;
    }

    public double getProbability() {
        return probability;
    }

    public String getTexture() {
        return texture;
    }

    public String getHeadName() {
        return headName;
    }
}
