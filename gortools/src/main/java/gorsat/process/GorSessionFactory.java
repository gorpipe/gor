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

package gorsat.process;

import org.gorpipe.gor.session.GenericFactory;
import org.gorpipe.gor.session.GorSession;

import java.nio.file.Paths;

public abstract class GorSessionFactory extends GenericFactory<GorSession> {

    protected static String updateCommonRoot(String commonRootOpt) {
        if (commonRootOpt == null) { // If not specified on command line, try the vm default
            commonRootOpt = System.getProperty("gor.common.root", "");
        }
        if (commonRootOpt != null) {
            if (commonRootOpt.trim().length() == 0) {
                // Default to the process's actual absolute CWD, not the
                // literal string "./" -- resolving a relative path
                // against "./" is a no-op in this codebase's URI-based
                // path resolution (PathUtils.resolve: resolving anything
                // against a relative base just returns it unchanged),
                // so anything that needed real anchoring against this
                // default quietly stayed relative/un-anchored instead
                // (e.g. PGOR's dictionary-folder write caching -- see
                // the .gord.link mechanism in GeneralQueryHandler.
                // getResultsLinkPath, which silently mis-resolves and
                // fails with a "Resource Error" on a fabricated
                // version_XXXX.gord name when this root isn't a real,
                // absolute path). This value is not used for access-
                // control decisions -- the real enforcement path
                // (DriverBackedSecureFileReader) always sources its own
                // root independently -- so this only affects path-
                // resolution/anchoring correctness, not security scoping.
                commonRootOpt = Paths.get("").toAbsolutePath().toString();
            }
            if (commonRootOpt.length() > 2 && commonRootOpt.charAt(1) == ':' && !commonRootOpt.endsWith("\\")) { // windows path hack
                commonRootOpt = commonRootOpt + '\\';
            } else if (!commonRootOpt.endsWith("/") && !commonRootOpt.endsWith("\\")) {
                commonRootOpt = commonRootOpt + '/';
            }
        }
        return commonRootOpt;
    }
}
