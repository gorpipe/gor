/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.querydialogs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class holding information on lists and their content. The lists are split into default lists and other lists.
 *
 * @version $Id$
 */
public class SubjectLists {
    private List<String> listsNames;
    private List<String[]> listsContent;
    private List<String> defaultListsNames;
    private List<String[]> defaultListsContent;
    private String[] listsColumnHeaders;
    private boolean useDefaultLists;

    /**
     * Constructor.
     */
    public SubjectLists() {
        this.listsNames = new ArrayList<>();
        this.listsContent = new ArrayList<>();
    }

    /**
     * Copy constructor.
     *
     * @param subjectLists the subject lists to copy
     */
    public SubjectLists(final SubjectLists subjectLists) {
        this.listsNames = new ArrayList<>(subjectLists.listsNames);
        this.listsContent = new ArrayList<>();
        for (String[] listCont : subjectLists.listsContent) {
            listsContent.add(Arrays.copyOf(listCont, listCont.length));
        }

        if (subjectLists.defaultListsNames != null) {
            this.defaultListsNames = new ArrayList<>(subjectLists.defaultListsNames);
        }

        if (subjectLists.defaultListsContent != null) {
            this.defaultListsContent = new ArrayList<>();
            for (String[] defListCont : subjectLists.defaultListsContent) {
                defaultListsContent.add(Arrays.copyOf(defListCont, defListCont.length));
            }
        }
        this.useDefaultLists = subjectLists.useDefaultLists;
    }

    /**
     * Set data for lists other than default.
     *
     * @param inpListsNames   lists names
     * @param inpListsContent lists content
     */
    public void setData(final List<String> inpListsNames, final List<String[]> inpListsContent) {
        this.listsNames = inpListsNames;
        this.listsContent = inpListsContent;
        useDefaultLists = false;
    }

    /**
     * Set data for default lists.
     *
     * @param inpDefaultListsNames   lists names
     * @param inpDefaultListsContent lists content
     * @param inpListsColumnHeaders  the column headers for lists
     */
    public void setDefaultListsData(final List<String> inpDefaultListsNames, final List<String[]> inpDefaultListsContent,
                                    final String[] inpListsColumnHeaders) {
        this.defaultListsNames = inpDefaultListsNames;
        this.defaultListsContent = inpDefaultListsContent;
        this.listsColumnHeaders = inpListsColumnHeaders;
        useDefaultLists = true;
    }

    /**
     * Check if default lists should be used.
     *
     * @return <code>true</code> if default lists should be used, <code>false</code> if other lists should be used
     */
    public boolean useDefaultLists() {
        return useDefaultLists;
    }

    @Override
    /**
     * Returns comma separated content subject values. The first value in each content entry is the subject value.
     * Output is either for default or other lists depending on the value of useDefaultLists().
     */
    public String toString() {
        String subjects = null;
        if (useDefaultLists && defaultListsContent.size() > 0) {
            subjects = joinSubjects(defaultListsContent);
        } else if (listsContent.size() > 0) {
            subjects = joinSubjects(listsContent);
        }
        return subjects;
    }

    /**
     * Returns content subject values. The first value in each content entry is the subject value.
     * Output is either for default or other lists depending on the value of useDefaultLists().
     *
     * @return content subject values
     */
    public String[] getListsSubjectContent() {
        if (useDefaultLists) {
            return getListsSubjectContent(defaultListsContent);
        }
        return getListsSubjectContent(listsContent);
    }

    /**
     * Get lists content.
     *
     * @return lists content
     */
    public String[][] getListsContent() {
        if (useDefaultLists) {
            return getListsContent(defaultListsContent);
        }
        return getListsContent(listsContent);
    }

    /**
     * Get the lists column headers.
     *
     * @return the lists column headers
     */
    public String[] getListsColumnHeaders() {
        return listsColumnHeaders;
    }

    /**
     * Get comma separated lists names excluding default lists.
     *
     * @return comma separated lists names
     */
    public String[] getListsNamesExcludeDefaultLists() {
        return listsNames.toArray(new String[listsNames.size()]);
    }

    /**
     * Get the names of lists in a comma separated string. Output is either for default or other lists
     * depending on the value of useDefaultLists().
     *
     * @return comma separated lists names
     */
    public String getListsNamesJoined() {
        if (useDefaultLists) {
            return String.join(",", defaultListsNames);
        }
        return String.join(",", listsNames);
    }

    private String joinSubjects(final List<String[]> content) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(content.get(i)[0]); // Value in first column is the subject
        }
        return sb.toString();
    }

    private String[] getListsSubjectContent(final List<String[]> content) {
        final String[] subjects = new String[content.size()];
        for (int i = 0; i < subjects.length; i++) {
            subjects[i] = content.get(i)[0]; // Value in first column is the subject
        }
        return subjects;
    }

    private String[][] getListsContent(final List<String[]> content) {
        return content.toArray(new String[content.size()][]);
    }
}
