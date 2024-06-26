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

public abstract class GorSessionFactory extends GenericFactory<GorSession> {

    protected static String updateCommonRoot(String commonRootOpt) {
        if (commonRootOpt == null) { // If not specified on command line, try the vm default
            commonRootOpt = System.getProperty("gor.common.root", "");
        }
        if (commonRootOpt != null) {
            if (commonRootOpt.trim().length() == 0) {
                commonRootOpt = "./";
            } else if (commonRootOpt.length() > 2 && commonRootOpt.charAt(1) == ':' && !commonRootOpt.endsWith("\\")) { // windows path hack
                commonRootOpt = commonRootOpt + '\\';
            } else if (!commonRootOpt.endsWith("/")) {
                commonRootOpt = commonRootOpt + '/';
            }
        }
        return commonRootOpt;
    }
}
