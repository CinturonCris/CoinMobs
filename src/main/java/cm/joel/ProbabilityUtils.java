package cm.joel;

import java.util.Random;

public class ProbabilityUtils {

    public static double convertToDecimal(int probability) {
        return (double) probability / 100;
    }

    public static boolean eventOccurs(int probability) {
        Random random = new Random();
        int randomValue = random.nextInt(100) + 1;
        return randomValue <= probability;
    }
}