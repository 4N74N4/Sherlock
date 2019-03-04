package com.bgu.agent.sensors;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.mit.media.funf.math.FFT;
import edu.mit.media.funf.math.MFCC;
import edu.mit.media.funf.math.Window;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

import java.util.Arrays;

/**
 * Created by shedan on 29/04/2015.
 */
@Probe.RequiredFeatures({"android.hardware.microphone"})
@Probe.RequiredPermissions({"android.permission.RECORD_AUDIO"})
public class AudioFeaturesProbe extends Probe.Base implements Probe.ContinuousProbe, ProbeKeys.AudioFeaturesKeys {
    private static int RECORDER_SOURCE = 6;
    private static int RECORDER_CHANNELS = 16;
    private static int RECORDER_AUDIO_ENCODING = 2;
    private static int RECORDER_SAMPLERATE = 8000;
    private static int FFT_SIZE = 8192;
    private static int MFCCS_VALUE = 12;
    private static int MEL_BANDS = 20;
    private static double[] FREQ_BANDEDGES = new double[]{50.0D, 250.0D, 500.0D, 1000.0D, 2000.0D};
    private Thread recordingThread = null;
    private int bufferSize = 0;
    private int bufferSamples = 0;
    private static int[] freqBandIdx = null;
    private FFT featureFFT = null;
    private MFCC featureMFCC = null;
    private Window featureWin = null;
    private AudioRecord audioRecorder = null;
    public double prevSecs = 0.0D;
    public double[] featureBuffer = null;

    public AudioFeaturesProbe() {}


    protected void onStart() {
        super.onStart();
        try {
            this.bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
            this.bufferSize = Math.max(this.bufferSize, RECORDER_SAMPLERATE * 2);
            this.bufferSamples = this.bufferSize / 2;
            this.featureFFT = new FFT(FFT_SIZE);
            this.featureWin = new Window(this.bufferSamples);
            this.featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, (double) RECORDER_SAMPLERATE);
            freqBandIdx = new int[FREQ_BANDEDGES.length];

            for (int i = 0; i < FREQ_BANDEDGES.length; ++i) {
                freqBandIdx[i] = Math.round((float) FREQ_BANDEDGES[i] * ((float) FFT_SIZE / (float) RECORDER_SAMPLERATE));
            }

            this.audioRecorder = new AudioRecord(RECORDER_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, this.bufferSize);
            this.prevSecs = (double) System.currentTimeMillis() / 1000.0D;
            this.audioRecorder.startRecording();
            this.recordingThread = new Thread(new Runnable() {
                public void run() {
                    AudioFeaturesProbe.this.handleAudioStream();
                }
            }, "AudioRecorder Thread");
            this.recordingThread.start();
        }
        catch (Exception ex){}
    }

    protected void onStop() {
        super.onStop();
        this.audioRecorder.stop();
        this.audioRecorder.release();
        this.audioRecorder = null;
        this.recordingThread = null;
    }

    private void handleAudioStream() {
        short[] data16bit = new short[this.bufferSamples];
        byte[] data8bit = new byte[this.bufferSize];
        double[] fftBufferR = new double[FFT_SIZE];
        double[] fftBufferI = new double[FFT_SIZE];
        double[] featureCepstrum = new double[MFCCS_VALUE];
        boolean readAudioSamples = false;

        while (State.RUNNING.equals(this.getState())) {
            try {
                int var21 = this.audioRecorder.read(data16bit, 0, this.bufferSamples);
                double currentSecs = (double) System.currentTimeMillis() / 1000.0D;
                double diffSecs = currentSecs - this.prevSecs;
                this.prevSecs = currentSecs;
                JsonObject data = new JsonObject();
                if (var21 > 0) {
                    double fN = (double) var21;
                    data.addProperty("diffSecs", Double.valueOf(diffSecs));

                    for (int accum = 0; accum < this.bufferSamples; ++accum) {
                        data8bit[accum * 2] = (byte) data16bit[accum];
                        data8bit[accum * 2 + 1] = (byte) (data16bit[accum] >> 8);
                    }

                    double var22 = 0.0D;

                    int psdAcrossFrequencyBands;
                    for (psdAcrossFrequencyBands = 0; psdAcrossFrequencyBands < var21; ++psdAcrossFrequencyBands) {
                        var22 += Math.abs((double) data16bit[psdAcrossFrequencyBands]);
                    }

                    data.addProperty("l1Norm", Double.valueOf(var22 / fN));
                    var22 = 0.0D;

                    for (psdAcrossFrequencyBands = 0; psdAcrossFrequencyBands < var21; ++psdAcrossFrequencyBands) {
                        var22 += (double) data16bit[psdAcrossFrequencyBands] * (double) data16bit[psdAcrossFrequencyBands];
                    }

                    data.addProperty("l2Norm", Double.valueOf(Math.sqrt(var22 / fN)));
                    var22 = 0.0D;

                    for (psdAcrossFrequencyBands = 0; psdAcrossFrequencyBands < var21; ++psdAcrossFrequencyBands) {
                        var22 = Math.max(Math.abs((double) data16bit[psdAcrossFrequencyBands]), var22);
                    }

                    data.addProperty("linfNorm", Double.valueOf(Math.sqrt(var22)));
                    Arrays.fill(fftBufferR, 0.0D);
                    Arrays.fill(fftBufferI, 0.0D);

                    for (psdAcrossFrequencyBands = 0; psdAcrossFrequencyBands < var21; ++psdAcrossFrequencyBands) {
                        fftBufferR[psdAcrossFrequencyBands] = (double) data16bit[psdAcrossFrequencyBands];
                    }

                    this.featureWin.applyWindow(fftBufferR);
                    this.featureFFT.fft(fftBufferR, fftBufferI);
                    double[] var24 = new double[FREQ_BANDEDGES.length - 1];

                    for (int gson = 0; gson < FREQ_BANDEDGES.length - 1; ++gson) {
                        int j = freqBandIdx[gson];
                        int k = freqBandIdx[gson + 1];
                        var22 = 0.0D;

                        for (int h = j; h < k; ++h) {
                            var22 += fftBufferR[h] * fftBufferR[h] + fftBufferI[h] * fftBufferI[h];
                        }

                        var24[gson] = var22 / (double) (k - j);
                    }

                    Gson var23 = this.getGson();
                    data.add("psdAcrossFrequencyBands", var23.toJsonTree(var24));
                    featureCepstrum = this.featureMFCC.cepstrum(fftBufferR, fftBufferI);
                    data.add("mfccs", var23.toJsonTree(featureCepstrum));
                    this.sendData(data);
                }
            }
            catch(Exception ex){
                JsonObject data = new JsonObject();
                data.addProperty("diffSecs", "-1");
                data.addProperty("l1Norm", "-1");
                data.addProperty("l2Norm", "-1");
                data.addProperty("linfNorm","-1");
            }
        }
    }
}


