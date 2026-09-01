package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLint;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Emits one baseline case per registered CALC function.
 *
 * The flag matrix reaches commands; functions were only ever covered where some
 * other case happened to mention one, which left most of the 208 registered
 * functions untouched. Signatures carry the argument types, so a call can be
 * built rather than guessed: "sv:iv2sv" becomes FN('a',1).
 *
 * Queries are rooted in norrows so no fixture is needed, and whether a call
 * succeeds or errors is not this generator's concern — either is behaviour worth
 * pinning.
 */
public final class FunctionMatrixGenerator {

    /**
     * Generated cases, the functions whose arguments could not be supplied, and the
     * ones screened out as non-deterministic.
     */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> unsupported;
        public final List<String> nonDeterministic;

        GenerationResult(List<CompatCase> cases, List<String> unsupported,
                         List<String> nonDeterministic) {
            this.cases = Collections.unmodifiableList(cases);
            this.unsupported = Collections.unmodifiableList(unsupported);
            this.nonDeterministic = Collections.unmodifiableList(nonDeterministic);
        }
    }

    /**
     * A sample literal per signature argument type.
     *
     * The registry writes argument types as the function-type names (String, Int,
     * Double, Long) plus sl for a string list; the value-type spellings (sv, iv,
     * dv, lv, bv) appear in FunctionTypes but hardly ever in a registration, and
     * are mapped too so that one showing up is not silently unsupported. Boolean
     * is written as a comparison because CALC has no boolean literal.
     */
    private static final Map<String, String> ARGUMENT_VALUES = new LinkedHashMap<>();

    static {
        ARGUMENT_VALUES.put("String", "'a'");
        ARGUMENT_VALUES.put("Int", "1");
        ARGUMENT_VALUES.put("Long", "1");
        ARGUMENT_VALUES.put("Double", "1.5");
        ARGUMENT_VALUES.put("Boolean", "1=1");
        ARGUMENT_VALUES.put("sl", "'a,b'");
        ARGUMENT_VALUES.put("sv", "'a'");
        ARGUMENT_VALUES.put("iv", "1");
        ARGUMENT_VALUES.put("lv", "1");
        ARGUMENT_VALUES.put("dv", "1.5");
        ARGUMENT_VALUES.put("bv", "1=1");
    }

    private FunctionMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory) {
        return generate(inventory, Exclusions.load());
    }

    public static GenerationResult generate(SurfaceInventory inventory,
                                            Exclusions exclusions) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        List<String> nonDeterministic = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry
                : inventory.functionSignatures().entrySet()) {
            String name = entry.getKey();
            // SYSTEM shells out and EVAL runs a nested query; both are left out
            // through inventory/exclusions.yml, with the reason written down.
            if (exclusions.excludesFunction(name)) {
                continue;
            }
            String call = null;

            // The simplest signature the sample values can satisfy: a function
            // overloaded on types needs only one call to be reached.
            for (String signature : entry.getValue()) {
                String args = argumentsFor(signature);
                if (args != null) {
                    call = name + "(" + args + ")";
                    break;
                }
            }

            if (call == null) {
                unsupported.add(name + " " + entry.getValue());
                continue;
            }

            String query = "norrows 1 | calc X " + call;

            // MAXMEM and OPENFILES report JVM state, CURRENTDATE the clock. Each is
            // stable within one process, so the runtime reproducibility probe — which
            // runs both attempts in the same JVM — cannot see them. The static list
            // and the probe cover each other's blind spots.
            String token = CaseLint.nonDeterministicToken(query);
            if (token != null) {
                nonDeterministic.add(name + " uses " + token);
                continue;
            }

            CompatCase c = new CompatCase();
            c.id = "fn." + name.toLowerCase(Locale.ROOT) + ".call";
            c.tier = "baseline";
            c.mode = "exact";
            c.behavior = "Generated: CALC function " + name;
            c.query = query;
            cases.add(c);
        }
        return new GenerationResult(cases, unsupported, nonDeterministic);
    }

    /**
     * The argument list for a signature, or null when a type has no sample value.
     *
     * A signature is argument types joined by ':' then '2' then the return type;
     * "e" means the function takes nothing.
     */
    private static String argumentsFor(String signature) {
        int returnAt = signature.lastIndexOf('2');
        if (returnAt < 0) {
            return null;
        }
        String argPart = signature.substring(0, returnAt);
        if (argPart.isEmpty() || argPart.equals("e")) {
            return "";
        }

        StringBuilder args = new StringBuilder();
        for (String type : argPart.split(":")) {
            String value = ARGUMENT_VALUES.get(type);
            if (value == null) {
                return null;
            }
            if (args.length() > 0) {
                args.append(',');
            }
            args.append(value);
        }
        return args.toString();
    }
}
