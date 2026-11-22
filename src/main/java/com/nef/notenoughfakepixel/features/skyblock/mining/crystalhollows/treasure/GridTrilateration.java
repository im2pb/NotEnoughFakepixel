/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.treasure;

import lombok.var;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.*;

public final class GridTrilateration {

    public static final class Sample {
        public final double ox, oy, oz, d;
        public Sample(double ox, double oy, double oz, double d) {
            this.ox = ox; this.oy = oy; this.oz = oz; this.d = d;
        }
    }

    public static final class Int3 {
        public final int x, y, z;
        public Int3(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public String toString() { return "Int3(" + x + "," + y + "," + z + ")"; }
        @Override public boolean equals(Object o){
            if(!(o instanceof Int3)) return false;
            Int3 i = (Int3) o;
            return x==i.x && y==i.y && z==i.z;
        }
        @Override public int hashCode(){ return (x*73856093) ^ (y*19349663) ^ (z*83492791); }
    }

    public static final class Result {
        public final Int3 best;
        public final double score;       
        public final double gapToNext;   
        public Result(Int3 best, double score, double gapToNext) {
            this.best = best; this.score = score; this.gapToNext = gapToNext;
        }
        @Override public String toString(){
            return best + "  score=" + String.format(Locale.US,"%.3f",score)
                    + "  gapToNext=" + String.format(Locale.US,"%.3f",gapToNext);
        }
    }

    public static Result estimate(List<Sample> samples, double epsilon) {
        return estimate(samples, epsilon,  (int)ceil(max(2, epsilon)),  2_000_000);
    }

    public static Result estimate(List<Sample> samples, double epsilon, int extraPad, int maxVolume) {
        if (samples == null || samples.size() < 3)
            throw new IllegalArgumentException("Need at least 3 samples; 4+ non-coplanar is recommended.");

        double[] p0 = lsqGuess(samples);

        int[] box = intersectedAABB(samples, epsilon);
        expandBox(box, extraPad);

        long volume = (long)(box[3]-box[0]+1)*(box[4]-box[1]+1)*(box[5]-box[2]+1);

        Iterable<Int3> candidates;
        if (volume <= maxVolume) {
            candidates = () -> new Iterator<Int3>() {
                int x = box[0], y = box[1], z = box[2];
                @Override public boolean hasNext(){ return x <= box[3]; }
                @Override public Int3 next(){
                    Int3 out = new Int3(x,y,z);
                    
                    if (++z > box[5]) { z = box[2]; if (++y > box[4]) { y = box[1]; ++x; } }
                    return out;
                }
            };
        } else {
            
            int cx = (int)round(p0[0]), cy = (int)round(p0[1]), cz = (int)round(p0[2]);
            double worstResidual = worstResidual(samples, p0);
            int r = (int)ceil(max(2*epsilon, worstResidual + epsilon));
            candidates = () -> new Iterator() {
                final int minX = max(box[0], cx - r), maxX = min(box[3], cx + r);
                final int minY = max(box[1], cy - r), maxY = min(box[4], cy + r);
                final int minZ = max(box[2], cz - r), maxZ = min(box[5], cz + r);
                int x = minX, y = minY, z = minZ;
                @Override public boolean hasNext(){ return x <= maxX; }
                @Override public Int3 next() {
                    while (true) {
                        Int3 out = new Int3(x,y,z);
                        
                        if (++z > maxZ) { z = minZ; if (++y > maxY) { y = minY; ++x; if (x>maxX) return out; } }
                        
                        double dx = out.x - p0[0], dy = out.y - p0[1], dz = out.z - p0[2];
                        if (dx*dx + dy*dy + dz*dz <= (double)r*r) return out;
                    }
                }
            };
        }

        Int3 best = null, second = null;
        double bestScore = Double.POSITIVE_INFINITY, secondScore = Double.POSITIVE_INFINITY;

        for (Int3 c : candidates) {
            double s = 0.0;
            for (Sample smp : samples) {
                double dx = c.x - smp.ox, dy = c.y - smp.oy, dz = c.z - smp.oz;
                double resid = abs(sqrt(dx*dx + dy*dy + dz*dz) - smp.d);
                double n = resid / epsilon;
                s += n*n;
                if (s >= secondScore) {
                    break;
                }
            }
            if (s < bestScore) { secondScore = bestScore; second = best; bestScore = s; best = c; }
            else if (s < secondScore) { secondScore = s; second = c; }
        }

        double gap = (second == null) ? Double.POSITIVE_INFINITY : (secondScore - bestScore);
        return new Result(best, bestScore, gap);
    }

    private static double[] lsqGuess(List<Sample> samples) {
        
        var r = samples.get(0);
        int m = samples.size() - 1;
        double a00=0,a01=0,a02=0,a11=0,a12=0,a22=0; 
        double v0=0,v1=0,v2=0;                      

        for (int i=1;i<samples.size();i++) {
            var s = samples.get(i);
            double rx = s.ox - r.ox, ry = s.oy - r.oy, rz = s.oz - r.oz;
            double bi = (s.ox*s.ox + s.oy*s.oy + s.oz*s.oz - s.d*s.d)
                    - (r.ox*r.ox + r.oy*r.oy + r.oz*r.oz - r.d*r.d);
            
            double ax = 2*rx, ay = 2*ry, az = 2*rz;

            a00 += ax*ax; a01 += ax*ay; a02 += ax*az;
            a11 += ay*ay; a12 += ay*az; a22 += az*az;
            v0  += ax*bi; v1  += ay*bi; v2  += az*bi;
        }

        double[][] A = {
                {a00, a01, a02},
                {a01, a11, a12},
                {a02, a12, a22}
        };
        double[] b = {v0, v1, v2};
        return solve3x3(A, b);
    }

    private static double[] solve3x3(double[][] A, double[] b) {
        
        int[] piv = {0,1,2};
        for (int k=0;k<3;k++){
            
            int max=k; double maxVal = abs(A[piv[k]][k]);
            for (int i=k+1;i<3;i++){ double v=abs(A[piv[i]][k]); if(v>maxVal){max=i;maxVal=v;} }
            int tmp=piv[k]; piv[k]=piv[max]; piv[max]=tmp;

            int r = piv[k];
            double pivot = A[r][k];
            if (abs(pivot) < 1e-12) return new double[]{0,0,0}; 
            
            for (int j=k;j<3;j++) A[r][j] /= pivot;
            b[r] /= pivot;
            
            for (int i=0;i<3;i++){
                if (i==r) continue;
                double f = A[i][k];
                if (f == 0) continue;
                for (int j=k;j<3;j++) A[i][j] -= f*A[r][j];
                b[i] -= f*b[r];
            }
        }
        
        double[] x = new double[3];
        System.arraycopy(b, 0, x, 0, 3);
        return x;
    }

    private static int[] intersectedAABB(List<Sample> samples, double epsilon) {
        
        int minX = Integer.MIN_VALUE/4, minY = Integer.MIN_VALUE/4, minZ = Integer.MIN_VALUE/4;
        int maxX = Integer.MAX_VALUE/4, maxY = Integer.MAX_VALUE/4, maxZ = Integer.MAX_VALUE/4;

        for (Sample s : samples) {
            double R = s.d + epsilon;
            int sxMin = (int)floor(s.ox - R), sxMax = (int)ceil(s.ox + R);
            int syMin = (int)floor(s.oy - R), syMax = (int)ceil(s.oy + R);
            int szMin = (int)floor(s.oz - R), szMax = (int)ceil(s.oz + R);
            minX = max(minX, sxMin); maxX = min(maxX, sxMax);
            minY = max(minY, syMin); maxY = min(maxY, syMax);
            minZ = max(minZ, szMin); maxZ = min(maxZ, szMax);
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            
            minX = minY = minZ = Integer.MAX_VALUE/4;
            maxX = maxY = maxZ = Integer.MIN_VALUE/4;
            for (Sample s : samples) {
                double R = s.d + epsilon;
                minX = min(minX, (int)floor(s.ox - R)); maxX = max(maxX, (int)ceil(s.ox + R));
                minY = min(minY, (int)floor(s.oy - R)); maxY = max(maxY, (int)ceil(s.oy + R));
                minZ = min(minZ, (int)floor(s.oz - R)); maxZ = max(maxZ, (int)ceil(s.oz + R));
            }
        }
        return new int[]{minX,minY,minZ,maxX,maxY,maxZ};
    }

    private static void expandBox(int[] box, int pad){
        box[0]-=pad; box[1]-=pad; box[2]-=pad;
        box[3]+=pad; box[4]+=pad; box[5]+=pad;
    }

    private static double worstResidual(List<Sample> samples, double[] p){
        double w = 0.0;
        for (Sample s : samples) {
            double dx = p[0]-s.ox, dy = p[1]-s.oy, dz = p[2]-s.oz;
            double resid = abs(sqrt(dx*dx + dy*dy + dz*dz) - s.d);
            if (resid > w) w = resid;
        }
        return w;
    }

    public static Sample fromBlockPos(BlockPos pos, double d){
        return new Sample(pos.getX(), pos.getY(), pos.getZ(), d);
    }

    public static Sample fromVector(Vec3 v, double d){
        return new Sample(v.xCoord, v.yCoord, v.zCoord, d);
    }
}
