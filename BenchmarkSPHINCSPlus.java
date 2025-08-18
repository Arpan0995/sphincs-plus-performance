package com.example.pqc;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.SPHINCSPlusParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;

/**
 * Benchmark the SPHINCS+ stateless hash‑based signature algorithm.
 *
 * This program measures the time taken for key generation, signing and
 * verification across several approved SPHINCS+ parameter sets (as listed
 * in FIPS 205).  It also prints the encoded sizes of public and private
 * keys and the resulting signature length.  The results help illustrate
 * the trade‑offs between small key size and very large signatures when
 * integrating SPHINCS+ into applications.
 */
public class BenchmarkSPHINCSPlus {
    /** Number of iterations per measurement.  Increase for more stable averages. */
    private static final int ITERATIONS = 10;

    public static void main(String[] args) throws Exception {
        // Register the Bouncy Castle PQC provider
        Provider provider = new BouncyCastlePQCProvider();
        Security.addProvider(provider);
        System.out.println("Using provider: " + provider.getName());
        System.out.println("Iterations per parameter: " + ITERATIONS);
        System.out.println();

        benchmarkSphincsPlus();
    }

    /**
     * Run benchmarks for the approved SPHINCS+ parameter sets.
     */
    private static void benchmarkSphincsPlus() throws Exception {
        System.out.println("=== SPHINCS+ Signature Benchmark ===");
        // Selected parameter sets (SHA2 small/fast and SHAKE variants could be tested similarly)
        SPHINCSPlusParameterSpec[] params = new SPHINCSPlusParameterSpec[] {
            SPHINCSPlusParameterSpec.sha2_128s,
            SPHINCSPlusParameterSpec.sha2_128f,
            SPHINCSPlusParameterSpec.sha2_192s,
            SPHINCSPlusParameterSpec.sha2_192f,
            SPHINCSPlusParameterSpec.sha2_256s,
            SPHINCSPlusParameterSpec.sha2_256f
        };
        byte[] message = "The quick brown fox jumps over the lazy dog".getBytes();
        for (SPHINCSPlusParameterSpec param : params) {
            String name = param.getName();
            System.out.println("\nParameter: " + name);
            long[] genTimes = new long[ITERATIONS];
            long[] signTimes = new long[ITERATIONS];
            long[] verifyTimes = new long[ITERATIONS];
            int pubKeyLen = 0;
            int privKeyLen = 0;
            int sigLen = 0;
            for (int i = 0; i < ITERATIONS; i++) {
                // Key pair generation
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
                kpg.initialize(param, new SecureRandom());
                long startGen = System.nanoTime();
                KeyPair pair = kpg.generateKeyPair();
                long endGen = System.nanoTime();
                genTimes[i] = endGen - startGen;
                if (i == 0) {
                    pubKeyLen = pair.getPublic().getEncoded().length;
                    privKeyLen = pair.getPrivate().getEncoded().length;
                }
                // Signing
                Signature signer = Signature.getInstance("SPHINCSPlus", "BCPQC");
                long startSign = System.nanoTime();
                signer.initSign(pair.getPrivate(), new SecureRandom());
                signer.update(message);
                byte[] signature = signer.sign();
                long endSign = System.nanoTime();
                signTimes[i] = endSign - startSign;
                if (i == 0) {
                    sigLen = signature.length;
                }
                // Verification
                Signature verifier = Signature.getInstance("SPHINCSPlus", "BCPQC");
                long startVerify = System.nanoTime();
                verifier.initVerify(pair.getPublic());
                verifier.update(message);
                boolean ok = verifier.verify(signature);
                long endVerify = System.nanoTime();
                verifyTimes[i] = endVerify - startVerify;
                if (!ok) {
                    throw new IllegalStateException("SPHINCS+ signature verification failed for " + name);
                }
            }
            double genAvg = nanosToMillis(average(genTimes));
            double signAvg = nanosToMillis(average(signTimes));
            double verifyAvg = nanosToMillis(average(verifyTimes));
            System.out.printf("Key generation  : %.3f ms (avg)\n", genAvg);
            System.out.printf("Signing         : %.3f ms (avg)\n", signAvg);
            System.out.printf("Verification    : %.3f ms (avg)\n", verifyAvg);
            System.out.println("Public key size : " + pubKeyLen + " bytes");
            System.out.println("Private key size: " + privKeyLen + " bytes");
            System.out.println("Signature size  : " + sigLen + " bytes");
        }
    }

    /**
     * Compute the average of an array of longs (nanoseconds).
     */
    private static double average(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return (double) sum / values.length;
    }

    /**
     * Convert nanoseconds to milliseconds.
     */
    private static double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0;
    }
}