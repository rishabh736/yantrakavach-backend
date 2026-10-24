package com.yantrakavach.backend.service;

/**
 * Deterministic pseudo-random generator — a direct port of the mulberry32-style generator used by the
 * frontend (src/lib/mock.ts: rng()), so seeded demo data looks the same kind of "random" on both sides.
 * Java's 32-bit int arithmetic wraps the same way JS's `| 0` coercion does, so the formula translates
 * almost line for line; the one difference is the explicit unsigned-conversion mask on the final division,
 * since Java ints are signed.
 */
public final class MulberryRng {

    private int seed;

    public MulberryRng(int seed) {
        this.seed = seed;
    }

    public double next() {
        seed = seed + 0x6D2B79F5;
        int t = seed;
        t = (t ^ (t >>> 15)) * (1 | seed);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        long unsigned = (t ^ (t >>> 14)) & 0xFFFFFFFFL;
        return unsigned / 4294967296.0;
    }

    public double range(double lo, double hi) {
        return lo + next() * (hi - lo);
    }

    /** FNV-1a hash, matching src/lib/mock.ts hashStr(), used to seed a MulberryRng from a string key. */
    public static int hashStr(String s) {
        int h = 2166136261;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        return h;
    }
}
