package org.gorpipe.compat;

import org.gorpipe.compat.gen.DocFlagCrossCheck;

import java.util.List;
import java.util.stream.Collectors;

/** Prints the diagnostics report. Gates nothing. */
public final class ReportMain {

    private ReportMain() {
    }

    public static void main(String[] args) {
        List<CompatCase> cases = CaseLoader.loadAll();
        SurfaceInventory inventory = SurfaceInventory.read();

        System.out.print(CoverageReport.of(cases, inventory).render());

        DocFlagCrossCheck.CrossCheckResult docs = DocFlagCrossCheck.run(inventory);
        System.out.printf("  DOCS      %d undocumented flag(s), %d phantom flag(s),"
                        + " %d page(s) checked%n",
                docs.undocumented.size(), docs.phantom.size(), docs.pagesParsed);

        List<String> uncovered = CoverageReport.of(cases, inventory).uncoveredCommands();
        if (!uncovered.isEmpty()) {
            System.out.println("  NEXT GAPS " + uncovered.stream().limit(10)
                    .collect(Collectors.toList()));
        }
    }
}
