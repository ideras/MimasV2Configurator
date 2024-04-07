/*
 [The "BSD licence"]
 Copyright (c) 2017 Ivan de Jesus Deras (ideras@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package mc.gui;

import java.awt.Rectangle;
import purejavacomm.*;
import java.io.File;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import mc.MimasV2ConfigDownloader;
import purejavacomm.SerialPort;

/**
 *
 * @author ideras
 */
public class ConfiguratorWindow extends javax.swing.JFrame implements ProgrammingProgressListener {

    /**
     * Creates new form ConfiguratorWindow
     */
    public ConfiguratorWindow() {
        initComponents();
        
        fileChooser = new JFileChooser();
        fileFilter = new FileNameExtensionFilter("FPGA Programming file (*.bit, *.bin)", new String[]{"bit", "bin"});

        configDownloader = null;
        loadSerialPortList();
        
        String sParam = prefs.get("Programming File", "");
        txtProgFile.setText(sParam);
        
        boolean b = prefs.getBoolean("Verify flash after programming", false);
        chkVerifyFlash.setSelected(b);
        
        int val = prefs.getInt("BPS", 0);
        cmbSpeed.setSelectedIndex(val);
        
        Rectangle bounds = this.getBounds();
        bounds.x = prefs.getInt("Window.Pos.X", 0);
        bounds.y = prefs.getInt("Window.Pos.Y", 0);
        
        setBounds(bounds);
    }
    
    private void loadSerialPortList() {
        
        cmbSerialPorts.removeAllItems();

        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            switch (port.getPortType()) {
                case CommPortIdentifier.PORT_SERIAL:
                    cmbSerialPorts.addItem(port.getName());
                    break;
                default:
                    break;
            }
        }
    }

    File openFile(String title) {
        String filePath = txtProgFile.getText().trim();
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setDialogTitle(title);
        fileChooser.setMultiSelectionEnabled(false);

        if (!filePath.isEmpty()) {
            fileChooser.setSelectedFile(new File(filePath));
        } else {
            fileChooser.setSelectedFile(new File("./*"));
        }

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        } else {
            return null;
        }
    }
    
    private void appQuit() {
        String fileName = txtProgFile.getText().trim();
        
        if (!fileName.isEmpty()) {
            prefs.put("Programming File", fileName);
        }
        prefs.putBoolean("Verify flash after programming", chkVerifyFlash.isSelected());
        prefs.putInt("BPS", cmbSpeed.getSelectedIndex());
        
        Rectangle bounds = this.getBounds();
        prefs.putInt("Window.Pos.X", bounds.x);
        prefs.putInt("Window.Pos.Y", bounds.y);
        System.exit(0);
    }
    
    /* 
     * Programming Progress Listener methods
     */
    @Override
    public void initProgress(int maxValue) {
        prgProgramming.setMaximum(maxValue);
        prgProgramming.setValue(0);
        prgProgramming.repaint();
    }

    @Override
    public void updateProgress(int value) {
        prgProgramming.setValue(value);
        prgProgramming.repaint();
    }
    
    @Override
    public void logMessage(String message) {
        String theMessage = txtConsole.getText() + message + "\n";
        txtConsole.setText(theMessage);
    }
    
    @Override
    public void errorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error Programming", JOptionPane.OK_OPTION);
    }
    
    @Override
    public void updateTitle(String message) {
        lblMessage.setText(message);
        logMessage(message);
    }
    
    @Override
    public void programmingDone() {
        serialPort.close();
        
        btnProgram.setEnabled(true);
        chkVerifyFlash.setEnabled(true);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        cmbSerialPorts = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        txtProgFile = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        chkVerifyFlash = new javax.swing.JCheckBox();
        btnProgram = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        prgProgramming = new javax.swing.JProgressBar();
        lblMessage = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtConsole = new javax.swing.JTextArea();
        btnRefresh = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        cmbSpeed = new javax.swing.JComboBox();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Mimas V2 Programming Tool");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1.setText("Serial Port: ");

        cmbSerialPorts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSerialPortsActionPerformed(evt);
            }
        });

        jLabel2.setText("Programming File:");

        btnBrowse.setText("Browse");
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        chkVerifyFlash.setText("Verify Flash after programming");

        btnProgram.setText("Program Board");
        btnProgram.setToolTipText("");
        btnProgram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProgramActionPerformed(evt);
            }
        });

        btnClose.setText("Close");
        btnClose.setToolTipText("Close the application");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        lblMessage.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lblMessage.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        txtConsole.setColumns(20);
        txtConsole.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        txtConsole.setRows(5);
        jScrollPane1.setViewportView(txtConsole);

        btnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/view_refresh.png"))); // NOI18N
        btnRefresh.setToolTipText("Reload available serial ports");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        jLabel3.setText("Speed (bps) :");

        cmbSpeed.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "19200", "115200" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cmbSerialPorts, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel3)
                                .addGap(2, 2, 2)
                                .addComponent(cmbSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(105, 105, 105)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(txtProgFile, javax.swing.GroupLayout.PREFERRED_SIZE, 453, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnBrowse)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(chkVerifyFlash)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(prgProgramming, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnProgram)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnClose)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(35, 35, 35))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnRefresh)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(cmbSerialPorts, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(cmbSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3)))
                        .addGap(18, 18, 18)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtProgFile, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnBrowse))
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkVerifyFlash)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(prgProgramming, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnProgram)
                    .addComponent(btnClose))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cmbSerialPortsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSerialPortsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbSerialPortsActionPerformed

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        File selectedFile = openFile("Choose a programming file");

        if (selectedFile != null) {
            txtProgFile.setText(selectedFile.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        appQuit();
    }//GEN-LAST:event_btnCloseActionPerformed

    private void btnProgramActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProgramActionPerformed
        String serialPortName = (String)cmbSerialPorts.getSelectedItem();
        
        if (serialPortName == null) {
            JOptionPane.showMessageDialog(this, "Please select a serial port before programming.", "Error", JOptionPane.OK_OPTION);
            return;
        }
        
        String filename = txtProgFile.getText().trim();
        
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a programming file first.", "Error", JOptionPane.OK_OPTION);
            return;
        }
        
        CommPortIdentifier serialPortIdent;
        try {
            serialPortIdent = CommPortIdentifier.getPortIdentifier(serialPortName);
        } catch (NoSuchPortException ex) {
            JOptionPane.showMessageDialog(this, "The port doesn't exists.  Perhaps you disconnected the board from the port.", "Error opening port", JOptionPane.OK_OPTION);
            return;
        }
        
        try {                        
            serialPort = (SerialPort) serialPortIdent.open("MimasV2ConfigDownloader", 2000);
            serialPort.enableReceiveTimeout(2000);
            serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            
        } catch (PortInUseException ex) {
            JOptionPane.showMessageDialog(this, "Error opening port perhaps you don't permission to use the port or\nthe port is already in use.", "Error", JOptionPane.OK_OPTION);           
            return;
        } catch (UnsupportedCommOperationException ex) {
            JOptionPane.showMessageDialog(this, "Unsupported operation", "Error opening port", JOptionPane.OK_OPTION);
            serialPort.close();
            return;
        }
        
        txtConsole.setText("");
        configDownloader = new MimasV2ConfigDownloader(serialPort, filename, this, chkVerifyFlash.isSelected());
        
        if (!configDownloader.boardIsMimasV2()) {
            JOptionPane.showMessageDialog(this, "Cannot detect a Mimas V2 board connected to the port.\nPlease check that the board is in programming mode.");
            configDownloader = null;
            serialPort.close();
            return;
        }
        
        btnProgram.setEnabled(false);
        chkVerifyFlash.setEnabled(false);
        Thread t = new Thread(configDownloader);
        t.start();
    }//GEN-LAST:event_btnProgramActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        loadSerialPortList();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        appQuit();
    }//GEN-LAST:event_formWindowClosing

    private final JFileChooser fileChooser;
    private final FileFilter fileFilter;
    private SerialPort serialPort;
    private MimasV2ConfigDownloader configDownloader;
    Preferences prefs = Preferences.userNodeForPackage(ConfiguratorWindow.class);

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnProgram;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JCheckBox chkVerifyFlash;
    private javax.swing.JComboBox cmbSerialPorts;
    private javax.swing.JComboBox cmbSpeed;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JProgressBar prgProgramming;
    private javax.swing.JTextArea txtConsole;
    private javax.swing.JTextField txtProgFile;
    // End of variables declaration//GEN-END:variables

}
