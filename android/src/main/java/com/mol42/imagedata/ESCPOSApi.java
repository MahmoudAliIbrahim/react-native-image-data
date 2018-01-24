package com.mol42.imagedata;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.BitSet;

public class ESCPOSApi {

    private final byte[] INITIALIZE_PRINTER = new byte[]{0x1B,0x40};
    private final String INITIALIZE_PRINTER_HEX = "1B40";

    private final byte[] PRINT_AND_FEED_PAPER = new byte[]{0x0A};
    private final String PRINT_AND_FEED_PAPER_HEX = "0A";

    private final byte[] SELECT_BIT_IMAGE_MODE = new byte[]{(byte)0x1B, (byte)0x2A};
    private final String SELECT_BIT_IMAGE_MODE_HEX = "1B2A";
    
    private final byte[] SET_LINE_SPACING = new byte[]{0x1B, 0x33};
    private final String SET_LINE_SPACING_HEX = "1B33";

    // private FileOutputStream printOutput;

    public int maxBitsWidth = 255;

    public ESCPOSApi() {
    }

    private byte[] buildPOSCommand(byte[] command, byte... args) {
        byte[] posCommand = new byte[command.length + args.length];

        System.arraycopy(command, 0, posCommand, 0, command.length);
        System.arraycopy(args, 0, posCommand, command.length, args.length);

        return posCommand;
    }

    private String buildPOSCommandHex(String command, String... args) {
        StringBuffer buf = new StringBuffer();
        //byte[] posCommand = new String[command.length + args.length];
        buf.append(command);
        for (int i = 0; i < args.length; i++) {
            buf.append(args[i]);
        }
        //System.arraycopy(command, 0, posCommand, 0, command.length);
        //System.arraycopy(args, 0, posCommand, command.length, args.length);
        System.out.println("buildPOSCommandHex " + buf.toString());
        return buf.toString();
    }

    private BitSet getBitsImageData(Bitmap image) {
        int threshold = 127;
        int index = 0;
        int dimenssions = image.getWidth() * image.getHeight();
        BitSet imageBitsData = new BitSet(dimenssions);

        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int color = image.getPixel(x, y);
                int  red = (color & 0x00ff0000) >> 16;
                int  green = (color & 0x0000ff00) >> 8;
                int  blue = color & 0x000000ff;
                int luminance = (int)(red * 0.3 + green * 0.59 + blue * 0.11);
                // System.out.println("luminance ->" + luminance);
                // imageBitsData.set(index, (luminance < threshold));
                imageBitsData.set(index, luminance < 127);
                index++;
            }
        }

        return imageBitsData;
    }

    public String buildPrintImageCommand(Bitmap image) {

        StringBuffer hexBuffer = new StringBuffer();
        BitSet imageBits = getBitsImageData(image);

        byte widthLSB = (byte)(image.getWidth() & 0xFF);
        //byte widthMSB = (byte)((image.getWidth() >> 16) & 0xFF);
        byte widthMSB = (byte)((image.getWidth() >> 8) & 0xFF);

        // COMMANDS
        String selectBitImageModeCommand = buildPOSCommandHex(SELECT_BIT_IMAGE_MODE_HEX, Integer.toHexString((byte) 33), Integer.toHexString(widthLSB), Integer.toHexString(widthMSB));
        String setLineSpacing24Dots = buildPOSCommandHex(SET_LINE_SPACING_HEX, Integer.toHexString((byte) 24));
        String setLineSpacing30Dots = buildPOSCommandHex(SET_LINE_SPACING_HEX, Integer.toHexString((byte) 30));

        hexBuffer.append(INITIALIZE_PRINTER_HEX);
        hexBuffer.append(setLineSpacing24Dots);

        int offset = 0;
        while (offset < image.getHeight()) {
            hexBuffer.append(selectBitImageModeCommand);

            int imageDataLineIndex = 0;
            byte[] imageDataLine = new byte[3 * image.getWidth()];

            for (int x = 0; x < image.getWidth(); ++x) {

                // Remember, 24 dots = 24 bits = 3 bytes.
                // The 'k' variable keeps track of which of those
                // three bytes that we're currently scribbling into.
                for (int k = 0; k < 3; ++k) {
                    byte slice = 0;

                    // A byte is 8 bits. The 'b' variable keeps track
                    // of which bit in the byte we're recording.
                    for (int b = 0; b < 8; ++b) {
                        // Calculate the y position that we're currently
                        // trying to draw. We take our offset, divide it
                        // by 8 so we're talking about the y offset in
                        // terms of bytes, add our current 'k' byte
                        // offset to that, multiple by 8 to get it in terms
                        // of bits again, and add our bit offset to it.
                        int y = (((offset / 8) + k) * 8) + b;

                        // Calculate the location of the pixel we want in the bit array.
                        // It'll be at (y * width) + x.
                        int i = (y * image.getWidth()) + x;

                        // If the image (or this stripe of the image)
                        // is shorter than 24 dots, pad with zero.
                        boolean v = false;
                        if (i < imageBits.length()) {
                            v = imageBits.get(i);
                        }
                        // Finally, store our bit in the byte that we're currently
                        // scribbling to. Our current 'b' is actually the exact
                        // opposite of where we want it to be in the byte, so
                        // subtract it from 7, shift our bit into place in a temp
                        // byte, and OR it with the target byte to get it into there.
                        slice |= (byte) ((v ? 1 : 0) << (7 - b));
                    }

                    imageDataLine[imageDataLineIndex + k] = slice;

                    // Phew! Write the damn byte to the buffer
                    //printOutput.write(slice);
                }

                imageDataLineIndex += 3;
            }

            for (int i=0; i<imageDataLine.length; i++) {
                hexBuffer.append(Integer.toHexString(imageDataLine[i]));
            }
            offset += 24;
            hexBuffer.append(PRINT_AND_FEED_PAPER_HEX);
        }

        hexBuffer.append(setLineSpacing30Dots);

        return hexBuffer.toString();
    }
}