package com.gyoung.ft8_app;

public class FT8Lib {

    static {
        System.loadLibrary("ft8");
    }

    // Message encoding/decoding
    public static class FT8Message {
        public String text;
        public int snr;
        public float timeOffset;
        public float freqOffset;

        public FT8Message(String text, int snr, float timeOffset, float freqOffset) {
            this.text = text;
            this.snr = snr;
            this.timeOffset = timeOffset;
            this.freqOffset = freqOffset;
        }
    }

    /**
     * Encode an FT8 message to audio samples
     * @param message Message text (e.g., "CQ K1ABC FN42")
     * @param freqHz Frequency offset in Hz (typically 1000-2000)
     * @return Audio samples as float array (180000 samples at 12kHz sample rate for 15 seconds)
     */
    public native float[] encodeMessage(String message, float freqHz);

    /**
     * Decode FT8 messages from audio samples
     * @param samples Audio samples (float array, 12kHz sample rate)
     * @param numSamples Number of samples
     * @return Array of decoded messages
     */
    public native FT8Message[] decodeMessages(float[] samples, int numSamples);

    /**
     * Pack a message into 77-bit payload
     * @param message Message text
     * @return 77-bit payload as byte array (10 bytes)
     */
    public native byte[] packMessage(String message);

    /**
     * Unpack 77-bit payload into message text
     * @param payload 77-bit payload (10 bytes)
     * @return Decoded message text
     */
    public native String unpackMessage(byte[] payload);

    /**
     * Initialize the FT8 decoder
     * @param maxCandidates Maximum number of decoding candidates
     */
    public native void initDecoder(int maxCandidates);

    /**
     * Clean up decoder resources
     */
    public native void cleanupDecoder();
}