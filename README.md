# SPHINCS+ Digital Signature Performance Benchmark (Java)

## Overview

**SPHINCS+** is a stateless, hash‑based digital signature scheme.  Unlike lattice‑based or number‑theoretic signatures, SPHINCS+ derives its security from the hardness of the underlying hash function and does not rely on any algebraic structure.  This approach offers strong resistance against both classical and quantum adversaries and makes SPHINCS+ attractive for long‑term security.  The scheme was introduced in 2015 and subsequently improved; it was selected by NIST as the basis for the **Stateless Hash‑Based Digital Signature Algorithm (SLH‑DSA)** standard (FIPS 205).  A small public key (32–64 bytes) and private key (64–128 bytes) come at the price of a **very large signature**: depending on the parameter set, signatures range from **7.8 KB to almost 50 KB**【940063901073622†L23-L39】【383393490679519†L1094-L1146】.

### Performance and integration challenges

* **Large signatures:**  While SPHINCS+ has tiny public and private keys, the signature is huge.  For example, the 128‑bit “fast” parameter set (sha2‑128f) uses a **32‑byte public key, 64‑byte secret key, and 17 KB signature**【940063901073622†L23-L39】.  FIPS 205 lists other sets: `SLH‑DSA‑SHA2‑128s` produces a 32‑byte public key and **7,856‑byte signature**, and `SLH‑DSA‑SHA2‑256f` outputs a 64‑byte public key and **49,856‑byte signature**【383393490679519†L1064-L1146】.  Such signatures dwarf those of classical schemes (e.g., **ECDSA signatures are ≈ 70–144 bytes** and RSA‑2048 signatures 256 bytes【940063901073622†L109-120】), increasing bandwidth and storage requirements.
* **Computation cost:**  SPHINCS+ signatures involve computing thousands of hashes.  Even though the algorithm can generate hundreds of signatures per second on a desktop CPU【940063901073622†L23-L29】, this is still slower than lattice‑based schemes like Dilithium and far slower than ECDSA for resource‑constrained devices.  Large signatures also increase the time to transmit and verify data.
* **Use cases:**  SPHINCS+ is designed for applications where long‑term security is essential and bandwidth constraints are less critical (e.g., firmware signing or document archiving).  For general‑purpose protocols such as TLS, lattice‑based signatures like Dilithium typically offer better performance.

This repository contains a Java project to benchmark SPHINCS+ signatures using Bouncy Castle’s post‑quantum provider.  It measures **key generation**, **signing** and **verification** across several approved parameter sets.  Results show the practical performance and reveal the trade‑offs between security level and signature size.

## Parameter sets and sizes

| Parameter (SLH‑DSA) | Security category | Public key (bytes) | Signature (bytes) | Notes |
|---|---|---|---|---|
| **sha2‑128s / shake‑128s** | 1 (128‑bit) | 32 | 7,856 | “small” variant with shortest signature【383393490679519†L1064-L1146】. |
| **sha2‑128f / shake‑128f** | 1 (128‑bit) | 32 | 17,088 | Fast variant; recommended by original SPHINCS+ paper; public key 32 B, private key 64 B【940063901073622†L23-L39】【383393490679519†L1094-L1111】. |
| **sha2‑192s / shake‑192s** | 3 (192‑bit) | 48 | 16,224 | Higher security with moderate signature size【383393490679519†L1112-L1120】. |
| **sha2‑192f / shake‑192f** | 3 (192‑bit) | 48 | 35,664 | Fast variant at 192 bit security【383393490679519†L1121-L1129】. |
| **sha2‑256s / shake‑256s** | 5 (256‑bit) | 64 | 29,792 | Strongest “small” variant【383393490679519†L1130-L1137】. |
| **sha2‑256f / shake‑256f** | 5 (256‑bit) | 64 | 49,856 | Strongest “fast” variant with huge signatures【383393490679519†L1130-L1146】. |

## Project structure

```
sphincs-plus-performance/
├── README.md
├── pom.xml              # Maven build file declaring dependencies
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── pqc/
                        └── BenchmarkSPHINCSPlus.java
```

### Dependencies

The project uses the **Bouncy Castle PQC provider**.  The `pom.xml` defines the following dependency:

```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15to18</artifactId>
    <version>1.78</version>
</dependency>
```

### Running the benchmark

1. **Install Java 17 or later**.  The program uses the Java Cryptography Architecture and Bouncy Castle.
2. **Clone this repository** and change into the project directory.
3. **Build with Maven**:
   ```sh
   mvn package
   ```
4. **Run the benchmark**:
   ```sh
   java -cp target/sphincs-plus-performance-1.0-SNAPSHOT.jar \
        com.example.pqc.BenchmarkSPHINCSPlus
   ```

The benchmark will loop through the chosen SPHINCS+ parameter sets, measure average times for key generation, signing and verification over multiple iterations, and print the encoded sizes of keys and signatures.  Use these results to gauge whether SPHINCS+ meets your application’s performance requirements.

## Example: SPHINCS+ signing and verification

Here is a simplified example of signing a message and verifying it with the SPHINCS+ `sha2_128f` parameter using Bouncy Castle:

```java
// Register the PQC provider
Security.addProvider(new BouncyCastlePQCProvider());
// Generate a key pair for SPHINCS+ (fast 128‑bit)
KeyPairGenerator kpg = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
kpg.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
KeyPair pair = kpg.generateKeyPair();

// Sign a message
byte[] message = "Hello, SPHINCS+!".getBytes(StandardCharsets.UTF_8);
Signature signer = Signature.getInstance("SPHINCSPlus", "BCPQC");
signer.initSign(pair.getPrivate(), new SecureRandom());
signer.update(message);
byte[] signature = signer.sign();

// Verify the signature
Signature verifier = Signature.getInstance("SPHINCSPlus", "BCPQC");
verifier.initVerify(pair.getPublic());
verifier.update(message);
boolean verified = verifier.verify(signature);
System.out.println("Signature verified? " + verified);
```

## Benchmark summary and comparison

SPHINCS+ offers **small keys** but **very large signatures**.  For the 128‑bit security level, the fast variant produces a 17 KB signature, while the small variant reduces this to 7.8 KB【383393490679519†L1094-L1111】.  At higher security levels (192‑bit and 256‑bit), signatures grow to 16–35 KB and 30–50 KB respectively【383393490679519†L1112-L1146】.  In comparison, **ECDSA** signatures are only 70–144 bytes and **RSA‑2048** signatures 256 bytes【940063901073622†L109-120】.  Although SPHINCS+ can generate hundreds of signatures per second on a desktop processor【940063901073622†L23-L29】, the computational load and bandwidth overhead make it less suited for latency‑sensitive protocols.

Nevertheless, SPHINCS+ provides a **stateless** alternative to Dilithium and Falcon and is valuable for applications where key reuse is undesirable and long‑term security is paramount.  Developers must weigh the trade‑offs between signature size and performance when integrating SPHINCS+ into their systems.