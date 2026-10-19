package jp.akkiserver.otogi.core.combat;

/** Pure geometry shared by hit detection and combat tuning tests. */
public final class CombatGeometry {
    /** A real point on the target box inside the aimed cone; never adds a hidden close-range rear hit. */
    public static org.bukkit.util.Vector coneContact(org.bukkit.util.BoundingBox box, org.bukkit.util.Vector origin,
                                                     org.bukkit.util.Vector direction, double reach, double halfAngle) {
        if (!Double.isFinite(reach) || reach <= 0 || !Double.isFinite(halfAngle)
                || !Double.isFinite(direction.lengthSquared()) || direction.lengthSquared() < 1e-10) return null;
        var forward = direction.clone().normalize();
        if (box.contains(origin)) return origin.clone();
        var direct = box.rayTrace(origin, forward, reach);
        if (direct != null) return direct.getHitPosition();
        double cosine = Math.cos(Math.toRadians(Math.clamp(halfAngle, 0, 180)));
        org.bukkit.util.Vector best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        double[] xs = {box.getMinX(), Math.clamp(origin.getX(), box.getMinX(), box.getMaxX()), box.getCenterX(), box.getMaxX()};
        double[] ys = {box.getMinY(), Math.clamp(origin.getY(), box.getMinY(), box.getMaxY()), box.getCenterY(), box.getMaxY()};
        double[] zs = {box.getMinZ(), Math.clamp(origin.getZ(), box.getMinZ(), box.getMaxZ()), box.getCenterZ(), box.getMaxZ()};
        for (double x : xs) for (double y : ys) for (double z : zs) {
            var ray = new org.bukkit.util.Vector(x, y, z).subtract(origin);
            if (ray.lengthSquared() < 1e-10 || ray.normalize().dot(forward) + 1e-9 < cosine) continue;
            var contact = box.rayTrace(origin, ray, reach);
            if (contact == null) continue;
            double distance = contact.getHitPosition().distanceSquared(origin);
            if (distance < bestDistance) { bestDistance = distance; best = contact.getHitPosition(); }
        }
        return best;
    }
    /** Whether the attacker lies in the horizontal rear cone. Coincident points have no rear. */
    public static boolean isBehind(double facingX, double facingZ, double deltaX, double deltaZ, double halfAngleDegrees) {
        double facingLength = Math.hypot(facingX, facingZ);
        double deltaLength = Math.hypot(deltaX, deltaZ);
        if (!Double.isFinite(facingLength) || !Double.isFinite(deltaLength) || !Double.isFinite(halfAngleDegrees)
                || facingLength < 0.000001 || deltaLength < 0.000001) return false;
        double dot = facingX / facingLength * (deltaX / deltaLength) + facingZ / facingLength * (deltaZ / deltaLength);
        return dot <= -Math.cos(Math.toRadians(Math.clamp(halfAngleDegrees, 0, 180))) + 1.0e-10;
    }

    public static double clampFinite(double value, double fallback, double min, double max) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    private CombatGeometry() {}
}
