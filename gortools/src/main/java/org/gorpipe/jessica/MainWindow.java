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

package org.gorpipe.jessica;

import gorsat.Commands.RowHeader;
import org.gorpipe.gor.model.Row;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;

public class MainWindow extends JFrame implements DocumentListener, ActionListener {
    private final transient GorQueryHandler gorQueryHandler;

    private final JTextArea input;
    private final JTextArea output;
    private final JCheckBox autoRun;
    private final JLabel status;
    private final DefaultTableModel tableData;

    public MainWindow(GorQueryHandler il)
    {
        super("Jessica");
        gorQueryHandler = il;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel inputLabel = new JLabel("Enter command:");

        input = new JTextArea();
        input.getDocument().addDocumentListener(this);

        autoRun = new JCheckBox("Auto-run", true);

        JButton runButton = new JButton("Run");
        runButton.setActionCommand("run");
        runButton.addActionListener(this);

        output = new JTextArea();
        output.setEditable(false);

        JScrollPane outputScroll = new JScrollPane(output);
        status = new JLabel("Status message");

        tableData = new DefaultTableModel();

        JTable table = new JTable(tableData);

        JScrollPane tableScroll = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        //Create a parallel group for the horizontal axis
        GroupLayout.ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);

        //Create a sequential and a parallel groups
        GroupLayout.SequentialGroup h1 = layout.createSequentialGroup();
        GroupLayout.ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);

        h1.addContainerGap();

        h2.addComponent(outputScroll, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
        h2.addComponent(tableScroll, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
        h2.addComponent(status, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);

        //Create a sequential group h3
        GroupLayout.SequentialGroup h3 = layout.createSequentialGroup();
        h3.addComponent(inputLabel);
        h3.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        h3.addComponent(input, GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE);
        h3.addComponent(runButton);
        h3.addComponent(autoRun);

        //Add the group h3 to the group h2
        h2.addGroup(h3);
        //Add the group h2 to the group h1
        h1.addGroup(h2);

        h1.addContainerGap();

        //Add the group h1 to the hGroup
        hGroup.addGroup(GroupLayout.Alignment.TRAILING, h1);
        //Create the horizontal group
        layout.setHorizontalGroup(hGroup);


        //Create a parallel group for the vertical axis
        GroupLayout.ParallelGroup vGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        //Create a sequential group v1
        GroupLayout.SequentialGroup v1 = layout.createSequentialGroup();
        //Add a container gap to the sequential group v1
        v1.addContainerGap();
        //Create a parallel group v2
        GroupLayout.ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        v2.addComponent(inputLabel);
        v2.addComponent(input, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE);
        v2.addComponent(runButton);
        v2.addComponent(autoRun);

        //Add the group v2 tp the group v1
        v1.addGroup(v2);
        v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        v1.addComponent(outputScroll, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE);
        v1.addComponent(tableScroll, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE);
        v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        v1.addComponent(status);
        v1.addContainerGap();

        //Add the group v1 to the group vGroup
        vGroup.addGroup(v1);
        //Create the vertical group
        layout.setVerticalGroup(vGroup);
        pack();
    }

    private void handleInput() {
        gorQueryHandler.setQuery(input.getText());
        boolean parseSuccessful = gorQueryHandler.parse();
        if(parseSuccessful) {
            if(autoRun.isSelected()) {
                runQueryAndShowResults();
            } else {
                clearResults();
                status.setText("OK");
            }
        } else {
            clearResults();
            status.setText("Error");
        }

        showPipeDescription();
    }

    private void clearResults() {
        tableData.setDataVector(new Vector<>(), new Vector<Vector>());
    }

    private void showPipeDescription() {
        PipeStepInfo[] pipeStepInfos = gorQueryHandler.getSteps();
        final String pipeDescription = getPipeDescription(pipeStepInfos);
        final Throwable exception = gorQueryHandler.getException();
        final String exceptionDescription = exception == null ? "" : exception.getMessage();
        output.setText(pipeDescription + exceptionDescription);
    }

    private void runQueryAndShowResults() {
        gorQueryHandler.run();

        RowHeader outputHeader = gorQueryHandler.getOutputHeader();
        if(outputHeader != null) {
            String[] namesWithTypes = outputHeader.columnNamesWithTypes();
            Vector<String> columnNames = new Vector<>(namesWithTypes.length);
            Collections.addAll(columnNames, namesWithTypes);

            Vector<Vector<String>> values = rowArrayToVector(gorQueryHandler.getResults());
            tableData.setDataVector(values, columnNames);

            final Throwable exception = gorQueryHandler.getException();
            if(exception != null) {
                status.setText("Exception thrown");
            } else {
                status.setText("OK");
            }
        } else {
            status.setText("No output header");
        }
    }

    private Vector<Vector<String>> rowArrayToVector(Row[] result) {
        Vector<Vector<String>> values = new Vector<>(result.length);
        for(Row r: result) {
            Vector<String> rowValues = rowToStringVector(r);
            values.add(rowValues);
        }
        return values;
    }

    private Vector<String> rowToStringVector(Row r) {
        String[] fields = r.getAllCols().toString().split("\t");
        Vector<String> rowValues = new Vector<>(fields.length);
        Collections.addAll(rowValues, fields);
        return rowValues;
    }

    private void handleRun() {
        runQueryAndShowResults();
    }

    private String getPipeDescription(PipeStepInfo[] pipeStepInfos) {
        StringBuilder sb = new StringBuilder();
        for(PipeStepInfo stepInfo: pipeStepInfos) {
            sb.append(stepInfo.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        handleInput();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        handleInput();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        //Plain text components do not fire these events
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if("run".equals(e.getActionCommand())) {
            handleRun();
        }
    }
}
