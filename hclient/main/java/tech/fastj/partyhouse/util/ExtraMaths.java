package tech.fastj.partyhouse.util;

/**
 * Extra math functions not supplied in the standard library or {@link tech.fastj.math.Maths}.
 */
public class ExtraMaths {

    /**
     * Scales the provided number {@code num} on a scale of {@code} to {@code 1} based on the provided {@code minimum} and {@code maximum}
     * values.
     *
     * @param num The number to normalize.
     * @param min The minimum value in a range of possible values for {@code num}.
     * @param max The maximum value in a range of possible values for {@code num}.
     * @return The normalized version of {@code num}, on a scale of {@code} to {@code 1}.
     */
    public static double normalize(long num, long min, long max) {
        return (double) (num - min) / (double) (max - min);
    }

    /**
     * Scales the provided number {@code num} on a scale of {@code 0.0} to {@code 1.0} based on the provided {@code minimum} and
     * {@code maximum} values.
     *
     * @param num The number to normalize.
     * @param min The minimum value in a range of possible values for {@code num}.
     * @param max The maximum value in a range of possible values for {@code num}.
     * @return The normalized version of {@code num}, on a scale of {@code 0.0} to {@code 1.0}.
     */
    public static float normalize(float num, float min, float max) {
        return (num - min) / (max - min);
    }
}
