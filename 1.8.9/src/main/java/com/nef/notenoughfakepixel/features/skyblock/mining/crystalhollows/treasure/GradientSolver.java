package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.treasure;

import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Primary:    gradient — places 2 initial candidates from the first pair.
 * Complement: sphere-projection refinement — iteratively pulls each candidate
 *             onto every reading's XZ circle, fixing gradient approximation error.
 * Validation: once narrowed to 1 candidate, each new reading checks whether
 *             the measured gradient (Δd/|ΔP|) agrees with the direction toward
 *             that candidate.  Three consecutive disagreements invalidate it and
 *             trigger fresh regeneration.
 * Stability:  once the solver has ever narrowed to 1 candidate, regeneration
 *             always picks the closest new candidate to that last known position,
 *             so candidate count never widens back from 1 to 2.
 */
public class GradientSolver {

    private static final double TOLERANCE      = 4.0;  // max allowed XZ residual
    private static final double MIN_MOVE       = 1.5;  // min XZ displacement between readings
    private static final int    REFINE_ITERS   = 10;   // sphere projection iterations
    private static final double CONVERGE_DIST  = 3.0;  // auto-merge if within this many blocks
    private static final double GRADIENT_AGREE = 0.35; // max cosθ error (dual-candidate discrimination only)
    private static final int    MAX_READINGS   = 16;   // sliding window
    private static final double SWITCH_SLACK   = 2.0;  // treasure-switch detection tolerance (blocks)

    private final List<double[]> readings   = new ArrayList<>();  // {px, py, pz, d}
    private final List<double[]> candidates = new ArrayList<>();  // {cx, cz}

    private int      dualDisagreeCount = 0;
    private int      dualDisagreeIdx   = -1; // -1=none, 0=A losing, 1=B losing
    private double   lastX = Double.NaN, lastZ = Double.NaN;
    private double[] lastSingleCandidate = null;

    // ── Public API ────────────────────────────────────────────────────────────

    public synchronized boolean addReading(double px, double py, double pz, double d) {
        if (!Double.isNaN(lastX)) {
            double dx = px - lastX, dz = pz - lastZ;
            double moveLenSq = dx * dx + dz * dz;
            if (moveLenSq < MIN_MOVE * MIN_MOVE) return false;

            // Treasure-switch detection: |Δd| > |ΔPos_3D| is geometrically impossible for a
            // fixed treasure.  Use 3D movement so Y-axis mining doesn't cause false triggers.
            if (!readings.isEmpty()) {
                double[] lastR    = readings.get(readings.size() - 1);
                double distChange = Math.abs(d - lastR[3]);
                double dx3 = px - lastR[0], dy3 = py - lastR[1], dz3 = pz - lastR[2];
                double move3D = Math.sqrt(dx3*dx3 + dy3*dy3 + dz3*dz3);
                if (distChange > move3D + SWITCH_SLACK) {
                    readings.clear();
                    candidates.clear();
                    dualDisagreeCount = 0;
                    dualDisagreeIdx   = -1;
                    lastSingleCandidate = null;
                }
            }
        }
        lastX = px; lastZ = pz;
        if (readings.size() >= MAX_READINGS) readings.remove(0);
        readings.add(new double[]{px, py, pz, d});
        if (readings.size() >= 2) recompute();
        return true;
    }

    public synchronized List<BlockPos> getCandidatePositions() {
        if (candidates.isEmpty()) return Collections.emptyList();
        double avgY = avgPlayerY();
        List<BlockPos> result = new ArrayList<>();
        for (double[] c : candidates)
            result.add(new BlockPos((int) Math.floor(c[0]), (int) Math.floor(avgY), (int) Math.floor(c[1])));
        return result;
    }

    public synchronized int getCandidateCount() { return candidates.size(); }
    public synchronized int getReadingCount()   { return readings.size(); }

    public synchronized void reset() {
        readings.clear();
        candidates.clear();
        dualDisagreeCount = 0;
        dualDisagreeIdx   = -1;
        lastX = lastZ = Double.NaN;
        lastSingleCandidate = null;
    }

    // ── Solver ────────────────────────────────────────────────────────────────

    private void recompute() {
        int n = readings.size();

        if (!candidates.isEmpty()) {

            // Single candidate — never drop it, never regenerate, just refine.
            // Sphere projection converges the position naturally; consistency checks
            // near the treasure are unreliable and cause the 1→2 regression.
            if (candidates.size() == 1) {
                double[] ref = refineWithSpheres(candidates.get(0)[0], candidates.get(0)[1]);
                lastSingleCandidate = new double[]{ref[0], ref[1]};
                candidates.set(0, ref);
                return;
            }

            // 2 candidates — try to narrow to 1 via consistency + gradient.
            List<double[]> reRefined = new ArrayList<>();
            for (double[] c : candidates) {
                double[] ref = refineWithSpheres(c[0], c[1]);
                if (consistentEnough(ref)) reRefined.add(ref);
            }

            if (!reRefined.isEmpty()) {
                List<double[]> merged = tryMerge(reRefined);

                if (merged.size() == 2 && n >= 3) {
                    boolean aDisagrees = gradientDisagrees(merged.get(0), n);
                    boolean bDisagrees = gradientDisagrees(merged.get(1), n);
                    if (aDisagrees && !bDisagrees) {
                        if (dualDisagreeIdx == 0) dualDisagreeCount++;
                        else { dualDisagreeIdx = 0; dualDisagreeCount = 1; }
                    } else if (bDisagrees && !aDisagrees) {
                        if (dualDisagreeIdx == 1) dualDisagreeCount++;
                        else { dualDisagreeIdx = 1; dualDisagreeCount = 1; }
                    } else {
                        dualDisagreeIdx = -1; dualDisagreeCount = 0;
                    }
                    if (dualDisagreeCount >= 2) {
                        double[] kept = (dualDisagreeIdx == 0) ? merged.get(1) : merged.get(0);
                        merged = Collections.singletonList(kept);
                        lastSingleCandidate = new double[]{kept[0], kept[1]};
                        dualDisagreeIdx = -1; dualDisagreeCount = 0;
                    }
                } else {
                    dualDisagreeIdx = -1; dualDisagreeCount = 0;
                }
                candidates.clear();
                candidates.addAll(merged);
                return;
            }

            candidates.clear();
            dualDisagreeCount = 0;
            dualDisagreeIdx   = -1;
        }

        // No candidates — generate from latest pair.
        List<double[]> newCands = generateFromPair(readings.get(n - 2), readings.get(n - 1));
        if (newCands.isEmpty()) return;

        List<double[]> result = new ArrayList<>();
        for (double[] c : newCands) {
            double[] ref = refineWithSpheres(c[0], c[1]);
            if (consistentWithMost(ref)) result.add(ref);
        }

        // Once locked to 1, always regenerate to exactly 1 — pick closest to last known position.
        if (lastSingleCandidate != null && result.size() > 1) {
            double[] best = result.get(0);
            double bestDsq = xzDistSq(best, lastSingleCandidate);
            for (double[] r : result) {
                double dsq = xzDistSq(r, lastSingleCandidate);
                if (dsq < bestDsq) { bestDsq = dsq; best = r; }
            }
            result = Collections.singletonList(best);
        }

        if (!result.isEmpty()) {
            candidates.clear();
            candidates.addAll(tryMerge(result));
        }
    }

    // ── Gradient validation ───────────────────────────────────────────────────

    /**
     * Returns true if the measured gradient disagrees with the direction to candidate c.
     *
     *   cosθ_expected = dot(direction_to_c, movement) / |movement|
     *   cosθ_actual   = -Δd / |ΔP|   (gradient formula: ∂d/∂û = -cosθ)
     *
     * A correct candidate should satisfy |cosθ_expected - cosθ_actual| < GRADIENT_AGREE.
     */
    private boolean gradientDisagrees(double[] c, int n) {
        double[] r1 = readings.get(n - 2);
        double[] r2 = readings.get(n - 1);

        double dxMove = r2[0] - r1[0], dzMove = r2[2] - r1[2];
        double moveLen = Math.sqrt(dxMove * dxMove + dzMove * dzMove);
        if (moveLen < MIN_MOVE) return false;

        double dxToC = c[0] - r1[0], dzToC = c[1] - r1[2];
        double distToC = Math.sqrt(dxToC * dxToC + dzToC * dzToC);
        if (distToC < 2.0) return false;

        double cosExpected = (dxMove * dxToC + dzMove * dzToC) / (moveLen * distToC);
        double cosActual   = Math.max(-1.0, Math.min(1.0, -(r2[3] - r1[3]) / moveLen));

        return Math.abs(cosExpected - cosActual) > GRADIENT_AGREE;
    }

    // ── Sphere refinement ─────────────────────────────────────────────────────

    /**
     * Projects candidate (cx, cz) onto each reading's XZ circle and averages.
     * Repeat for REFINE_ITERS — converges toward the true intersection.
     */
    private double[] refineWithSpheres(double cx, double cz) {
        double avgY = avgPlayerY();
        for (int iter = 0; iter < REFINE_ITERS; iter++) {
            double sumX = 0, sumZ = 0;
            int cnt = 0;
            for (double[] r : readings) {
                double dx = cx - r[0], dz = cz - r[2];
                double xzDist = Math.sqrt(dx * dx + dz * dz);
                if (xzDist < 0.5) continue;
                double dY  = avgY - r[1];
                double xzR = Math.sqrt(Math.max(1.0, r[3] * r[3] - dY * dY));
                sumX += r[0] + (dx / xzDist) * xzR;
                sumZ += r[2] + (dz / xzDist) * xzR;
                cnt++;
            }
            if (cnt == 0) break;
            cx = sumX / cnt;
            cz = sumZ / cnt;
        }
        return new double[]{cx, cz};
    }

    // ── Gradient candidate generation ─────────────────────────────────────────

    private static List<double[]> generateFromPair(double[] r1, double[] r2) {
        double dx = r2[0] - r1[0], dz = r2[2] - r1[2];
        double moveLen = Math.sqrt(dx * dx + dz * dz);
        if (moveLen < 1e-6) return Collections.emptyList();

        double dd = r2[3] - r1[3];
        // |Δd| > moveLen is geometrically impossible for a fixed treasure — skip
        if (Math.abs(dd) >= moveLen) return Collections.emptyList();

        double cosTheta = Math.max(-1.0, Math.min(1.0, -dd / moveLen));
        double theta    = Math.acos(cosTheta);
        double bearing  = Math.atan2(dz, dx);
        double d1       = r1[3];

        List<double[]> result = new ArrayList<>(2);
        result.add(new double[]{r1[0] + d1 * Math.cos(bearing + theta),
                                r1[2] + d1 * Math.sin(bearing + theta)});
        result.add(new double[]{r1[0] + d1 * Math.cos(bearing - theta),
                                r1[2] + d1 * Math.sin(bearing - theta)});
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<double[]> tryMerge(List<double[]> cands) {
        if (cands.size() < 2) return cands;
        double dx = cands.get(0)[0] - cands.get(1)[0];
        double dz = cands.get(0)[1] - cands.get(1)[1];
        if (dx * dx + dz * dz <= CONVERGE_DIST * CONVERGE_DIST) {
            return Collections.singletonList(new double[]{
                (cands.get(0)[0] + cands.get(1)[0]) / 2,
                (cands.get(0)[1] + cands.get(1)[1]) / 2
            });
        }
        return cands;
    }

    private boolean consistent(double[] c, double[] r) {
        double dx = c[0] - r[0], dz = c[1] - r[2];
        double xzDist = Math.sqrt(dx * dx + dz * dz);
        double dY  = avgPlayerY() - r[1];
        double xzR = Math.sqrt(Math.max(1.0, r[3] * r[3] - dY * dY));
        return Math.abs(xzDist - xzR) <= TOLERANCE;
    }

    /** Strict: used for newly generated candidates — allows at most 1 inconsistent reading. */
    private boolean consistentWithMost(double[] c) {
        int ok = 0;
        for (double[] r : readings) if (consistent(c, r)) ok++;
        return ok >= readings.size() - 1;
    }

    /** Lenient: used when re-checking existing candidates — allows up to 25% noisy readings.
     *  Prevents a good candidate from being dropped due to 2–3 slightly off readings. */
    private boolean consistentEnough(double[] c) {
        int ok = 0;
        for (double[] r : readings) if (consistent(c, r)) ok++;
        int maxFailures = Math.max(1, readings.size() / 4);
        return ok >= readings.size() - maxFailures;
    }

    private static double xzDistSq(double[] a, double[] b) {
        double dx = a[0] - b[0], dz = a[1] - b[1];
        return dx * dx + dz * dz;
    }

    private double avgPlayerY() {
        if (readings.isEmpty()) return 64;
        double s = 0;
        for (double[] r : readings) s += r[1];
        return s / readings.size();
    }
}
