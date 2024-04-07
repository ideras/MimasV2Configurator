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
package mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import mc.gui.ProgrammingProgressListener;
import purejavacomm.SerialPort;

/**
 *
 * @author ideras
 */
public class MimasV2ConfigDownloader implements Runnable {

    public MimasV2ConfigDownloader(SerialPort serialPort, String fileName, ProgrammingProgressListener listener, boolean verifyFlash) {
        this.serialPort = serialPort;
        this.fileName = fileName;
        this.listener = listener;
        this.verifyFlash = verifyFlash;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(SerialPort serialPort) {
        this.serialPort = serialPort;
    }
    
    private void doDelay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
        }
    }
    
    /*
     * @description The lowest level routine to send raw data to Mimas V2
     * @retusn Returns total number of writes written
     */
    public int sendData(byte[] data) throws IOException {
        int i = 0;
        int bytesWritten = 0;
        //Send data 30 bytes at a time. Mimas V2 can recieve maximum 30 bytes per transaction
        while (i < data.length) {
            int endPos = ((i + 30) < data.length)? (i + 30) : data.length;
            
            byte [] dataToSend = Arrays.copyOfRange(data, i, endPos);
            serialPort.getOutputStream().write(dataToSend);
            
            bytesWritten += dataToSend.length;
            i += 30;
        }
        return bytesWritten;
    }
    
    public byte[] readData(int count) throws IOException {
        byte [] buffer = new byte[count];
        serialPort.getInputStream().read(buffer);
        return buffer;
    }
    
    /*
     * Send a command to Mimas V2
     * This routine will add padding to make all commands 70 bytes long
    */
    public void sendCommand(byte [] cmd) throws IOException, ConfiguratorException {
        byte [] command = cmd;
        
        if(cmd.length < 70) {
            command = new byte[70];
            
            Arrays.fill(command, (byte)' ');
            System.arraycopy(cmd, 0, command, 0, cmd.length);
        }
        
        int result = sendData(command);
        
        if (result != 70)
            throw new ConfiguratorException("Error while sending command. Bytes sent " + result + ", expected to send 70.");
    }
    
    /*
     * Set up SPI peripheral inside PIC18 chip on Mimas V2
     */
    public void spiOpen() throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum, SyncMode, BusMode, SmpPhase 
        //                        ~    , 0x00      , 0x01  , 0x02    , 0x00   , 0x00 
        sendCommand(new byte[]{0x7e, 0x00, 0x01, 0x02, 0x00, 0x00});
    }
    
    /*
     * Deinitialize and free resources allocated with SpiOpen command
     */
    public void spiClose() throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum
        //                        ~    , 0x01      , 0x01  
        sendCommand(new byte[]{0x7e, 0x01, 0x01});
    }

    /*
     * Set direction of IOs that are needed for configuration process
     */
    public void spiSetIoDirection(int io, int direction) throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum, Io, Direction
        //                        ~    , 0x08      , 0x01  , io, direction
        sendCommand(new byte[]{0x7e, 0x08, 0x01, (byte)io, (byte)direction});
    }
    
    /*
     * Set value of IOs that are needed for configuration process
     */
    public void spiSetIoValue(int io, int value) throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum, Io, Value
        //                        ~    , 0x09      , 0x01  , io, value
        sendCommand(new byte[]{0x7e, 0x09, 0x01, (byte)io, (byte)value});
    }
    
    /*
     * Flush input buffer of the port
     */
    public void flushInBuffer() throws IOException {
        doDelay(IN_BUFFER_FLUSH_DELAY);
                
        int bytesAvailable = serialPort.getInputStream().available();
        if (bytesAvailable > 0) {
            serialPort.getInputStream().skip(bytesAvailable);
        }
    }
    
    /*
     * Checks the satus of the last command sent. Use this routine only with commands
     * that returns generic status response.
     */
    public int checkStatus(byte[] lastCmd) throws IOException {

        /* 
         * Try to read 100 bytes from the input buffer. The maximum amount of data expected
         * is 38 bytes. If we receive more than 38 bytes, that means the input buffer has
         * response from more than one commands. This is means input buffer is not flushed 
         * before sending the last command. Input buffer can flushed by either calling 
         * FlushInBuffer() routine or by reading large enough data from the input buffer.
         * In most cases, simply calling CheckStatus() should clear the input buffer.
         */
        
        byte [] buffer = new byte[128];
        int bytesRead = serialPort.getInputStream().read(buffer, 0, 128);
         
        if (bytesRead > 38)
            return 1;
        else {
            if ((buffer[0] == 0x7e) && buffer[1] == (byte)CONFIG_IN_PACKET_STATUS &&
                 buffer[3] == 0) {
                if (lastCmd == null)
                    return 0;
                else
                    return (buffer[4] == lastCmd[0])? 0 : 1;
            } else {
                return 1;
            }
        }
    }
    
    /*
     * Toggles Chip Select
     */
    public void toggleCS() throws ConfiguratorException, IOException {
        //Set CS to output
        spiSetIoDirection(CONFIG_IO_PIN_CS, IO_DIRECTION_OUT);

        //De-assert CS
        spiSetIoValue(CONFIG_IO_PIN_CS, 1);

        //Assert CS
        spiSetIoValue(CONFIG_IO_PIN_CS, 0);
    }
    
    /*
     * Writes a character to SPI port
     */
    public void spiSendByte(int ch) throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum, Char
        //                        ~    , 0x07      , 0x01  , Char
        sendCommand(new byte[]{0x7e, 0x07, 0x01, (byte) ch});
    }
    
    /*
     * Writes a string/buffer to SPI port
     */
    public void spiSendBytes(byte[] buffer) throws ConfiguratorException, IOException {
        // Packet Structure : Sync Byte, PacketType, SpiNum, Char
        //                        ~    , 0x03      , 0x01  , Length, Res0, Res1, data
        byte[] dataToSend = new byte[buffer.length + 6];
        dataToSend[0] = 0x7e;
        dataToSend[1] = 0x03;
        dataToSend[2] = 0x01;
        dataToSend[3] = (byte) buffer.length;
        dataToSend[4] = 0x0;
        dataToSend[5] = 0x0;
        System.arraycopy(buffer, 0, dataToSend, 6, buffer.length);

        sendCommand(dataToSend);
    }
    
    /*
     * Reads a string/buffer from SPI
     */
    public byte[] getBytes(int length) throws ConfiguratorException, IOException {
        // Send CONFIG_OUT_PACKET_SPI_GETSTRING command
        // Packet Structure : Sync Byte, PacketType, SpiNum, Length
        //                        ~    , 0x02      , 0x01  , Length
        sendCommand(new byte[]{0x7e, 0x02, 0x01, (byte) length});

        // Read the response and extract data
        byte[] buffer = new byte[128];
        int bytesRead = serialPort.getInputStream().read(buffer, 0, 128);

        if (bytesRead != 38) {
            throw new ConfiguratorException("Error on get bytes: expected byte count 38, received " + bytesRead);
        } else {
            return Arrays.copyOfRange(buffer, 6, 6 + length);
        }
    }
    
    /*
     * Reads flash ID using command 9Fh
     */
    public int flashReadID9Fh() throws ConfiguratorException, IOException {
        //Toggle CS to get SPI flash to a known state
        toggleCS();

        //Write command 9Fh
        spiSendByte(SPI_FLASH_READ_ID_9F);

        // Flush input buffer 
        flushInBuffer();

        //Read three bytes from SPI flash
        byte[] data = getBytes(3);

        return (((int) data[0]) & 0xff) | (((int) data[1] << 8) & 0xff00) | (((int) data[2] << 16) & 0xff0000);
    }
    
    /*
     * Enable write for SPI flash M25P16
     */
    public void flashM25P16WriteEnable() throws ConfiguratorException, IOException {
        // Toggle CS to get SPI flash to a known state
        toggleCS();

        // Send write enable code
        spiSendByte(M25P16_WRITE_ENABLE);
        
        // De-assert CS
        spiSetIoValue(CONFIG_IO_PIN_CS, 1);
    }
    
    /*
     * Reads flash M25P16 Status register
     */
    public int flashM25P16ReadStatus() throws ConfiguratorException, IOException {
        // Toggle CS to get SPI flash to a known state
        toggleCS();

        // Write M25P16_READ_STATUS command
        spiSendByte(M25P16_READ_STATUS);

        // Flush input buffer 
        flushInBuffer();

        // Read one byte from SPI flash
        byte[] data = getBytes(1);

        return (int) data[0];
    }

    /*
     * Erases sectors up to the sector that contains EndAddress
     */
    public void flashM25P16sectorErase(int endAddress) throws ConfiguratorException, IOException {
        endAddress |= 0xFFFF;

        listener.initProgress(endAddress);
        
        for (int i = 0; i < endAddress; i += 0xFFFF) {

            // Do write enable
            flashM25P16WriteEnable();

            // Toggle CS to get SPI flash to a known state
            toggleCS();

            // Send Sector Erase command
            spiSendByte(M25P16_SECTOR_ERASE);
            
            // Send address			
            byte [] addressBytes = {(byte)((i >> 16) & 0xff), (byte)((i >> 8) & 0xff), (byte)(i & 0xff)};
            //spiSendBytes(addressBytes);
            spiSendByte(addressBytes[0]);
            spiSendByte(addressBytes[1]);
            spiSendByte(addressBytes[2]);
            
            // De-assert CS
            spiSetIoValue(CONFIG_IO_PIN_CS, 1);

            // Wait for sector erase to complete
            while ((flashM25P16ReadStatus() & 0x01) == 0x01) {
               doDelay(10);
            }
            listener.updateProgress(i + 0xFFFF);
        }
    }
    
    /*
     * SPI Flash page program
     */
    public void flashM25P16PageProgram(byte[] buffer, int address) throws ConfiguratorException, IOException {
        if (buffer.length > 0x100) {
            throw new ConfiguratorException("Buffer size too big while programming M25P16 flash.  Expected size 512, found " + buffer.length);
        }

        // Do write enable
        flashM25P16WriteEnable();

        // Toggle CS to get SPI flash to a known state
        toggleCS();

        // Send page program command
        spiSendByte(M25P16_PAGE_PROGRAM);

        // Send address
        byte [] addressBytes = {(byte)((address >> 16) & 0xff), (byte)((address >> 8) & 0xff), (byte)(address & 0xff)};
        //spiSendBytes(addressBytes);
        spiSendByte(addressBytes[0]);
        spiSendByte(addressBytes[1]);
        spiSendByte(addressBytes[2]);

        // Send data 64 bytes at a time
        int i = 0, count;
        int length = buffer.length;
        while (length != 0) {
            count = (length > 64) ? 64 : length;
            spiSendBytes(Arrays.copyOfRange(buffer, i, i + count));
            
            i += count;
            length -= count;
        }

        // De-assert CS
        spiSetIoValue(CONFIG_IO_PIN_CS, 1);
    }

    /*
     * Reads the contents of flash and compare with data in buffer
     */
    public boolean flashM25P16VerifyFlash(byte[] buffer) throws ConfiguratorException, IOException {
        // Toggle CS to get SPI flash to a known state
        toggleCS();

        // Send page program command
        spiSendByte(M25P16_READ);

        // Send address bytes
        spiSendByte(0);
        spiSendByte(0);
        spiSendByte(0);
        //spiSendBytes(new byte[]{0, 0, 0});

        // Flush input buffer 
        flushInBuffer();

        int readLength = buffer.length;
        
        listener.initProgress(readLength);
        int progressCount = 0, pos = 0;
        
        while (readLength != 0) {
            int count = (readLength > 32) ? 32 : readLength;
            byte[] block1 = Arrays.copyOfRange(buffer, pos, pos + count);
            byte[] block2 = getBytes(count);
            
            if (!Arrays.equals(block1, block2))
                return false;
            
            progressCount += block1.length;
            
            readLength -= count;
            pos += count;
            listener.updateProgress(progressCount);
        }

        return true;
    }
    
    /*
     * This method verify the board connected in the serial port is a Mimas V2
    */
    public boolean boardIsMimasV2() {
        try {
            // Set PROGB to output
            spiSetIoDirection(CONFIG_IO_PIN_PROGB, IO_DIRECTION_OUT);

            // Pull PROGB Low while Flash is being programmed
            spiSetIoValue(CONFIG_IO_PIN_PROGB, 0);

            // Open SPI port
            spiOpen();
            
            int id = flashReadID9Fh();
            
            // De-assert PROGB
            spiSetIoValue(CONFIG_IO_PIN_PROGB, 1);

            return (id == DEV_ID_MICRON_M25P16);
        } catch (ConfiguratorException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }
    
    /*
     * Configures Mimas V2
     */   
    @Override
    public void run() {
        try {
            // Set PROGB to output
            spiSetIoDirection(CONFIG_IO_PIN_PROGB, IO_DIRECTION_OUT);
            
            // Pull PROGB Low while Flash is being programmed
            spiSetIoValue(CONFIG_IO_PIN_PROGB, 0);
            
            // Open SPI port
            spiOpen();
            
            int id = flashReadID9Fh();
            int flashAlgorithm;
            
            if (id == DEV_ID_MICRON_M25P16) {
                listener.logMessage("Micron M25P16 SPI Flash detected");
                flashAlgorithm = FLASH_ALGORITHM_M25P16;
            } else {
                listener.programmingDone();
                listener.errorMessage("Unknown flash part: '" + Integer.toHexString(id) + "'");
                return;
            }
            // Execute device specific programming algorithm
            if (flashAlgorithm == FLASH_ALGORITHM_M25P16) {
                
                // Open the binary file and load the contents to buffer
                listener.logMessage("Loading file " + fileName + "...");
                File file = new File(fileName);
                
                FileInputStream in = new FileInputStream(file);
                
                // Find out the size of the file
                int fileSize = (int) file.length();
                
                // Read file in to buffer
                byte[] dataBuff = new byte[fileSize];
                in.read(dataBuff);
                in.close();
                
                // Erase flash sectors
                listener.updateTitle("Erasing flash sectors...");
                flashM25P16sectorErase(fileSize);
                
                int address = 0;
                
                listener.updateTitle("Programming FPGA Board ...");
                listener.initProgress(fileSize);
                
                while (fileSize != 0) {
                    int count = (fileSize > 0x100) ? 0x100 : fileSize;
                    flashM25P16PageProgram(Arrays.copyOfRange(dataBuff, address, address + count), address);
                    address += count;
                    fileSize -= count;
                    
                    // Wait for page program to complete
                    while ((flashM25P16ReadStatus() & 0x01) == 0x01) {
                        doDelay(10);
                    }
                    
                    listener.updateProgress(address);
                }
                
                if (verifyFlash) {
                    // Verify the flash contents
                    listener.updateTitle("Verifying flash contents...");

                    if (flashM25P16VerifyFlash(dataBuff)) {
                        listener.logMessage("Flash verification successful...");
                    } else {
                        listener.logMessage("Flash verification failed...");
                    }
                }
                listener.updateTitle("Programming done!");
                listener.logMessage("Resetting FPGA Board ...");
                
                // Set CS to input
                spiSetIoDirection(CONFIG_IO_PIN_CS, IO_DIRECTION_IN);
                doDelay(20);
                // De-assert PROGB
                spiSetIoValue(CONFIG_IO_PIN_PROGB, 1);
                doDelay(20);

                listener.programmingDone();
            }
        } catch (ConfiguratorException ex) {
            listener.programmingDone();
            listener.errorMessage(ex.getMessage());
        } catch (IOException ex) {
            listener.programmingDone();
            listener.errorMessage(ex.getMessage());
        }
    }
        
    /* Private fields */
    private SerialPort serialPort;
    private String fileName;
    private ProgrammingProgressListener listener;
    private boolean verifyFlash;

    /* Constants */
    public static final int ERROR_FILE_TOO_LARGE = 0xEFFF0001;
    public static final int MAX_PORTS = 100;

    public static final int CDC_DATA_OUT_EP_SIZE = 70;
    public static final int CDC_DATA_IN_EP_SIZE = 38;

    public static final int IN_BUFFER_FLUSH_DELAY = 10;

    public static final int IO_DIRECTION_OUT = 0;
    public static final int IO_DIRECTION_IN = 1;

    public static final int MODE_00 = 0x00; // Setting for SPI bus Mode 0,0
    public static final int MODE_01 = 0x01; // Setting for SPI bus Mode 0,1
    public static final int MODE_10 = 0x02; // Setting for SPI bus Mode 1,0
    public static final int MODE_11 = 0x03; // Setting for SPI bus Mode 1,1

    public static final int SPI_FOSC_64 = 0x02;
    public static final int SMPMID = 0x00;

    public static final int CONFIG_OUT_PACKET_SPI_OPEN = 0;
    public static final int CONFIG_OUT_PACKET_SPI_CLOSE = 1;
    public static final int CONFIG_OUT_PACKET_SPI_GETSTRING = 2;
    public static final int CONFIG_OUT_PACKET_SPI_PUTSTRING = 3;
    public static final int CONFIG_OUT_PACKET_SPI_GETSTRING_ATADDRESS = 4;
    public static final int CONFIG_OUT_PACKET_SPI_PUTSTRING_ATADDRESS = 5;
    public static final int CONFIG_OUT_PACKET_SPI_GET_CHAR = 6;
    public static final int CONFIG_OUT_PACKET_SPI_PUT_CHAR = 7;
    public static final int CONFIG_OUT_PACKET_SPI_SET_IO_DIR = 8;
    public static final int CONFIG_OUT_PACKET_SPI_SET_IO_VALUE = 9;
    public static final int CONFIG_OUT_PACKET_SPI_GET_IO_VALUE = 10;
    public static final int CONFIG_OUT_PACKET_SPI_GET_ALL_IO_VALUES = 11;

    public static final int CONFIG_IN_PACKET_STATUS = 0;
    public static final int CONFIG_IN_PACKET_BUFFER = 1;

    public static final int CONFIG_IO_PIN_SI = 0;
    public static final int CONFIG_IO_PIN_SO = 1;
    public static final int CONFIG_IO_PIN_CS = 2;
    public static final int CONFIG_IO_PIN_CLK = 3;
    public static final int CONFIG_IO_PIN_PROGB = 4;
    public static final int CONFIG_IO_PIN_DONE = 5;
    public static final int CONFIG_IO_PIN_INITB = 6;

    public static final int DEV_ID_M45PE10VMN6P = 0x114020;
    public static final int DEV_ID_ATMEL_AT45DB021D = 0x231F;
    public static final int DEV_ID_ATMEL_AT45DB161D = 0x261F;
    public static final int DEV_ID_MICRON_M25P16 = 0x152020;

    public static final int FLASH_ALGORITHM_M45PE10VMN6P = 0x01;
    public static final int FLASH_ALGORITHM_ATMEL_DATAFLASH = 0x02;
    public static final int FLASH_ALGORITHM_M25P16 = 0x03;

    //Generic SPI flash commands
    public static final int SPI_FLASH_READ_ID_9F = 0x9F;

    //Atmel Dataflash specific
    public static final int ATMEL_DATAFLASH_READ = 0x03;
    public static final int ATMEL_DATAFLASH_READ_STATUS = 0xD7;
    public static final int ATMEL_DATAFLASH_BUFFER_WRITE = 0x84;
    public static final int ATMEL_DATAFLASH_PAGE_PROGRAM = 0x83;

    //M45PE10VMN6P Specific
    public static final int M45PE10VMN6P_WRITE_ENABLE = 0x06;
    public static final int M45PE10VMN6P_WRITE_DISABLE = 0x04;
    public static final int M45PE10VMN6P_READ_ID = 0x9F;
    public static final int M45PE10VMN6P_READ_STATUS = 0x05;
    public static final int M45PE10VMN6P_READ = 0x03;
    public static final int M45PE10VMN6P_FAST_READ = 0x0B;
    public static final int M45PE10VMN6P_PAGE_WRITE = 0x0A;
    public static final int M45PE10VMN6P_PAGE_PROGRAM = 0x02;
    public static final int M45PE10VMN6P_PAGE_ERASE = 0xDB;
    public static final int M45PE10VMN6P_SECTOR_ERASE = 0xD8;
    public static final int M45PE10VMN6P_DEEP_PWR_DOWN = 0xB9;
    public static final int M45PE10VMN6P_REL_DEEP_PWR_DOWN = 0xB9;

    //M25P16 Specific
    public static final int M25P16_WRITE_ENABLE = 0x06;
    public static final int M25P16_WRITE_DISABLE = 0x04;
    public static final int M25P16_READ_ID = 0x9F;
    public static final int M25P16_READ_STATUS = 0x05;
    public static final int M25P16_READ = 0x03;
    public static final int M25P16_FAST_READ = 0x0B;
    public static final int M25P16_PAGE_PROGRAM = 0x02;
    public static final int M25P16_SECTOR_ERASE = 0xD8;
    public static final int M25P16_BULK_ERASE = 0xC7;
    public static final int M25P16_DEEP_PWR_DOWN = 0xB9;
    public static final int M25P16_REL_DEEP_PWR_DOWN = 0xAB;
}
