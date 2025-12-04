package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import picocli.CommandLine;

final class LinkStreamSourceProvider {

    private LinkStreamSourceProvider() {
    }

    static StreamSource resolve(String linkPath, String securityContext, String commonRoot, boolean writeable, Object commandInstance) {
        var fileReader = new DriverBackedFileReader(securityContext, commonRoot);
        var dataSource = fileReader.resolveUrl(linkPath, writeable);
        if (dataSource == null) {
            throw new CommandLine.ExecutionException(new CommandLine(commandInstance),
                    "Could not resolve link file path: " + linkPath);
        }
        if (dataSource instanceof StreamSource streamSource) {
            return streamSource;
        }
        throw new CommandLine.ExecutionException(new CommandLine(commandInstance),
                "Link path is not stream compatible: " + linkPath);
    }
}

