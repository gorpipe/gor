/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.querydialogs.templating;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JVM wide cache of compiled freemarker templates, keyed on the configuration, template name and full template source.
 * <p>
 * A compiled {@link Template} is safe to process from many threads at once, as long as all per-render state (method
 * models with state, exception handlers that count errors, ...) is set on the per-render
 * {@link freemarker.core.Environment} rather than on the shared {@link Configuration} or the template.
 * Because the key contains the full source, an edited template always compiles to a new entry.
 */
public final class TemplateCache {
    private static final int MAX_TEMPLATES = Integer.getInteger("gor.dialog.template.cache.size", 2000);
    private static final Cache<Key, Template> TEMPLATES = Caffeine.newBuilder().maximumSize(MAX_TEMPLATES).build();
    private static final AtomicLong COMPILES = new AtomicLong();

    private record Key(Configuration configuration, String name, String source) {
        // Configuration does not override equals/hashCode, so it is compared by identity.
    }

    private TemplateCache() {
    }

    /**
     * Get the compiled template for the given source, compiling it with the given configuration if needed.
     *
     * @param configuration configuration to compile with, must not be modified after first use
     * @param name          template name (used in error messages)
     * @param source        template source
     * @return compiled template
     * @throws IOException if the template can not be parsed ({@link freemarker.core.ParseException})
     */
    public static Template get(Configuration configuration, String name, String source) throws IOException {
        try {
            return TEMPLATES.get(new Key(configuration, name, source), k -> {
                try {
                    COMPILES.incrementAndGet();
                    return new Template(k.name(), k.source(), k.configuration());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * @return number of template compilations done by this cache, for tests and diagnostics
     */
    public static long compileCount() {
        return COMPILES.get();
    }
}
