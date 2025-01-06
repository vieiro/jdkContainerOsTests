import java.security.*;
import java.util.Arrays;
import java.util.List;

public class CheckAlgorithms {
    private static final List<String> possibleFirstArgs = Arrays.asList("assert", "true", "silent-assert", "list", "false");
    private static final List<String> possibleSecondArgs = Arrays.asList("algorithms", "providers", "both");
    private static final List<String> possibleThirdArgs = Arrays.asList("el8", "el9");

    // general purpose variables used later
    public static List<String> FIPS_PROVIDERS;
    public static List<String> NONFIPS_PROVIDERS;
    public static List<String> FIPS_ALGORITHMS;
    public static List<String> NONFIPS_ALGORITHMS;

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3 || args[0].equals("--help") || args[0].equals("-h")) {
            System.err.println("Test for listing available algorithms and providers and checking their FIPS compatibility");
            System.err.println("Usage: CheckAlgorithms " + possibleFirstArgs + " " + possibleSecondArgs + " " + possibleThirdArgs);
            System.err.println("First argument: specify whether to check FIPS compatibility (assert/true) or just list the items (list/false)");
            System.err.println("                silent-asserts just asserts, not lists the items");
            System.err.println("Second argument: specify what to check - algorithms, providers or both");
            System.err.println("Third argument: specify the operating system for the checking, RHEL 8 or 9");
            System.err.println("                optional, defaults to RHEL 8");
            System.exit(1);
        }

        // Parse the arguments
        String shouldHonorFips = args[0].toLowerCase();
        String testCategory = args[1].toLowerCase();
        String operatingSystem = "";
        if (args.length == 3) {
            operatingSystem = args[2].toLowerCase();
        }

        // Check if the shouldHonorFips is valid value
        if (!possibleFirstArgs.contains(shouldHonorFips)) {
            System.err.println("Invalid value for the first argument: '" + args[0] + "', use --help for more info.");
            System.exit(1);
        }

        // Check if the testCategory is valid value
        if (!possibleSecondArgs.contains(testCategory)) {
            System.err.println("Invalid test category: '" + args[1] + "', use --help for more info.");
            System.exit(1);
        }

        // Check if the operatingSystem is valid value
        if (!operatingSystem.isEmpty() && !possibleThirdArgs.contains(operatingSystem)) {
            System.err.println("Invalid operating system: '" + args[2] + "', use --help for more info.");
            System.exit(1);
        }

        System.out.println("Test type: " + shouldHonorFips);
        System.out.println("What is tested: " + testCategory);
        System.out.println("Operating system: " + operatingSystem);

        boolean listItems = !shouldHonorFips.equals("silent-assert");
        boolean honorFipsHere = shouldHonorFips.equals("assert") || shouldHonorFips.equals("true") || shouldHonorFips.equals("silent-assert");

        if (operatingSystem.equals("el9")) {
            FIPS_PROVIDERS = EL9_FIPS_PROVIDERS;
            NONFIPS_PROVIDERS = EL9_NONFIPS_PROVIDERS;
            FIPS_ALGORITHMS = EL9_FIPS_ALGORITHMS;
            NONFIPS_ALGORITHMS = EL9_NONFIPS_ALGORITHMS;
        } else {
            FIPS_PROVIDERS = EL8_FIPS_PROVIDERS;
            NONFIPS_PROVIDERS = EL8_NONFIPS_PROVIDERS;
            FIPS_ALGORITHMS = EL8_FIPS_ALGORITHMS;
            NONFIPS_ALGORITHMS = EL8_NONFIPS_ALGORITHMS;
        }

        boolean algorithmsOk = true;
        boolean providersOk = true;
        if (testCategory.equals("algorithms") || testCategory.equals("both")){
            algorithmsOk = checkAlgorithms(listItems, honorFipsHere);
        }
        if (testCategory.equals("providers") || testCategory.equals("both")) {
            providersOk = checkProviders(listItems, honorFipsHere);
        }

        // throwing the correct exception based on what failed (even if both failed)
        if (!algorithmsOk && !providersOk) {
            throw new Exception("Both algorithms and providers contain wrong or don't contain correct items.");
        } else if (!algorithmsOk) {
            throw new Exception("Algorithms contain wrong or don't contain correct items.");
        } else if (!providersOk) {
            throw new Exception("Providers contain wrong or don't contain correct items.");
        }
    }

    static boolean checkProviders(boolean listItems, boolean shouldHonorFips) {
        System.out.println(">>>CHECKING PROVIDERS<<<");

        // print all providers
        if (listItems) {
            System.out.println("LIST OF PROVIDERS:");
            for(Provider provider : Security.getProviders()){
                System.out.println("  " + provider);
            }
        }

        if (shouldHonorFips) {
            System.out.println("ASSERTING FIPS PROVIDERS:");
            return providerAssert();
        }

        return true; // no assertion = everything is ok
    }

    static boolean checkAlgorithms(boolean listItems, boolean shouldHonorFips) throws Exception{
        System.out.println(">>>CHECKING ALGORITHMS<<<");

        // print all algorithms
        if (listItems) {
            System.out.println("LIST OF ALGORITHMS:");
            for (String cipher : CipherList.getCipherList()) {
                System.out.println("  " + cipher);
            }
        }

        if (shouldHonorFips) {
            System.out.println("ASSERTING FIPS ALGORITHMS:");
            return algorithmAssert();
        }

        return true; // no assertion = everything is ok
    }

    static boolean algorithmAssert() throws Exception {
        List<String> algorithms = Arrays.asList(CipherList.getCipherList());
        boolean allOk = true;

        // assert that algorithms list contains all FIPS_ALGORITHMS
        for (String fips : FIPS_ALGORITHMS) {
            System.out.print("  asserting algorithms contain '" + fips + "' - ");
            if (algorithms.contains(fips)) {
                System.out.println("OK");
            } else {
                System.out.println("FAIL");
                allOk = false;
            }
        }

        // assert that algorithms list doesn't contain any NONFIPS_ALGORITHMS
        for (String nonFips : NONFIPS_ALGORITHMS) {
            System.out.print("  asserting algorithms don't contain '" + nonFips + "' - ");
            if (!algorithms.contains(nonFips)) {
                System.out.println("OK");
            } else {
                System.out.println("FAIL");
                allOk = false;
            }
        }

        return allOk;
    }

    static boolean providerAssert() {
        Provider[] providers = Security.getProviders();
        boolean allOk = true;

        // assert that providers list contains all FIPS_PROVIDERS
        for (String fips : FIPS_PROVIDERS) {
            System.out.print("  asserting providers contain '" + fips + "' - ");

            allOk = false;
            for (Provider p : providers) {
                if (p.contains(fips)) {
                    System.out.println("OK");
                    allOk = true;
                    break;
                }
            }

            if (!allOk) {
                System.out.println("FAIL");
            }
        }

        // assert that providers list doesn't contain any NONFIPS_PROVIDERS
        for (String nonFips : NONFIPS_PROVIDERS) {
            System.out.print("  asserting providers don't contain '" + nonFips + "' - ");

            for (Provider p : providers) {
                if (p.contains(nonFips)) {
                    System.out.println("FAIL");
                    allOk = false;
                    break;
                }
            }

            if (allOk) {
                System.out.println("OK");
            }
        }

        return allOk;
    }

    // the algorithm and providers fips and non-fips values:
    public static final List<String> EL8_FIPS_PROVIDERS = Arrays.asList("SunPKCS11-NSS-FIPS");
    public static final List<String> EL8_NONFIPS_PROVIDERS = Arrays.asList("SunJGSS", "SunSASL", "SunPCSC", "JdkLDAP", "JdkSASL", "SunPKCS11");
    public static final List<String> EL8_FIPS_ALGORITHMS = Arrays.asList();
    public static final List<String> EL8_NONFIPS_ALGORITHMS = Arrays.asList("TLS_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA");

    public static final List<String> EL9_FIPS_PROVIDERS = Arrays.asList("SunPKCS11-NSS-FIPS");
    public static final List<String> EL9_NONFIPS_PROVIDERS = Arrays.asList("SunJGSS", "SunSASL", "SunPCSC", "JdkLDAP", "JdkSASL", "SunPKCS11");
    public static final List<String> EL9_FIPS_ALGORITHMS = Arrays.asList();
    public static final List<String> EL9_NONFIPS_ALGORITHMS = Arrays.asList("TLS_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA");
}

