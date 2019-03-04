package com.bgu.agent.commons.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/7/13
 * Time: 9:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {
    private static final String DOUBLE_LINE_SEP = "\n\n";
    private static final String SINGLE_LINE_SEP = "\n";
    private static final String EXTRA_CRASHED_FLAG = "CRUSH";

    public static String getReport(Throwable e, String addedInformation) {
        StackTraceElement[] arr = e.getStackTrace();
        final StringBuffer report = new StringBuffer(e.toString());
        final String lineSeperator = "-------------------------------\n\n";
        report.append(DOUBLE_LINE_SEP);
        report.append("--------- Stack trace ---------\n\n");
        for (int i = 0; i < arr.length; i++) {
            report.append("    ");
            report.append(arr[i].toString());
            report.append(SINGLE_LINE_SEP);
        }
        report.append(lineSeperator);
        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report.append("--------- Cause ---------\n\n");
        Throwable cause = e.getCause();
        if (cause != null) {
            report.append(cause.toString());
            report.append(DOUBLE_LINE_SEP);
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report.append("    ");
                report.append(arr[i].toString());
                report.append(SINGLE_LINE_SEP);
            }
        }
        // Getting the Device brand,model and sdk verion details.
        report.append(lineSeperator);
        report.append(getDeviceInfo());
        report.append(lineSeperator);
        report.append(addedInformation);
        return report.toString();
    }

    public static String getDeviceInfo() {
        final String lineSeperator = "-------------------------------\n\n";
        StringBuffer report = new StringBuffer();
        report.append("--------- Device ---------\n\n");
        report.append("Brand: ");
        report.append(Build.BRAND);
        report.append(SINGLE_LINE_SEP);
        report.append("Device: ");
        report.append(Build.DEVICE);
        report.append(SINGLE_LINE_SEP);
        report.append("Model: ");
        report.append(Build.MODEL);
        report.append(SINGLE_LINE_SEP);
        report.append("Id: ");
        report.append(Build.ID);
        report.append(SINGLE_LINE_SEP);
        report.append("Product: ");
        report.append(Build.PRODUCT);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);
        report.append("--------- Firmware ---------\n\n");
        report.append("SDK: ");
        report.append(Build.VERSION.SDK);
        report.append(SINGLE_LINE_SEP);
        report.append("Release: ");
        report.append(Build.VERSION.RELEASE);
        report.append(SINGLE_LINE_SEP);
        report.append("Incremental: ");
        report.append(Build.VERSION.INCREMENTAL);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);
        return report.toString();
    }

    public static Uri getLogcatLog(Context context) {
        Uri logcatURI = null;
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v time dalvikvm:V *:I");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            File logcatDir = getAlbumStorageDir("logcat");
            File logcatFile = new File(logcatDir, "logcat.txt");
            PrintWriter printWriter = new PrintWriter(logcatFile);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                printWriter.println(line);
            }
            printWriter.close();
            bufferedReader.close();
            logcatURI = Uri.fromFile(logcatFile);
        } catch (IOException ex) {
        }
        return logcatURI;
    }

    public static File getAlbumStorageDir(String albumName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File path = new File(sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/Data");

        if (!path.exists() || !path.isDirectory()) {
            Logger.i(Utils.class, "Directory created");
            path.mkdirs();
        }
        return path;
    }

    public static void sendEmail(Context context, String error, Uri logcatURI, String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Logcat report");
        intent.putExtra(Intent.EXTRA_TEXT, error + "\n\n" + "Logcat " + new Date().toString() + "\n\n" + getDeviceInfo());
        if (logcatURI != null) {
            intent.putExtra(Intent.EXTRA_STREAM, logcatURI);
        }
        intent.setData(Uri.parse("mailto:" + email)); // or just "mailto:" for blank
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Intent emailIntent = Intent.createChooser(intent, "Logcat report :");
        //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
        context.startActivity(intent);
    }

    public static void sendEmailAndExit(Context context, String report, Uri logcatURI, String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Error report");
        intent.putExtra(Intent.EXTRA_TEXT, report.toString());
        if (logcatURI != null) {
            intent.putExtra(Intent.EXTRA_STREAM, logcatURI);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("mailto:" + email)); // or just "mailto:" for blank
        //Intent emailIntent = Intent.createChooser(intent, "Unexpected Error occurred, choose an email client for sending error report :");
        //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
        context.startActivity(intent);//emailIntent);
        System.exit(0);
    }

    public static void writeFile(Context context, byte[] data, String fileName) {
        try {
            OutputStream output = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            output.write(data);
            output.close();
        } catch (Exception e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);
        }
    }

    public static boolean fileExists(Context context, String fileName) {
        File imageFile = context.getFileStreamPath(fileName);
        return imageFile.exists();
    }

    public static void deleteImages(Context context) {
        String[] list = context.getFileStreamPath(".").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                if (fileName.startsWith("Business_"))
                    return true;
                return false;
            }
        });
        for (String i : list)
            new File(i).delete();
    }

    public static String getExistingImages(Context context) {
        String[] list = context.getFileStreamPath(".").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                if (fileName.startsWith("Business_"))
                    return true;
                return false;
            }
        });
        StringBuffer returnVal = new StringBuffer();
        for (int i = 0; i < list.length; i++) {
            returnVal.append(list[i].substring(9));
            returnVal.append(",");
        }
        if (returnVal.length() > 0)
            returnVal.setLength(returnVal.length() - 1);
        return returnVal.toString();
    }

    public static String getService(String serviceMethod) {
        return serviceMethod.substring(0, serviceMethod.indexOf("_"));
    }

    public static String getMethod(String serviceMethod) {
        return serviceMethod.substring(serviceMethod.indexOf("_") + 1);
    }


    public static boolean isOnline(Context applicationContext, boolean includePing) {
        ConnectivityManager cm =
                (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            if (includePing) {
                URLConnection conn = null;
                try {
                    conn = URI.create("http://www.google.com").toURL().openConnection();
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
                    InputStream in = conn.getInputStream();
                    in.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                return true;
        }
        return false;
    }

    public static <T> T getView(View view, int id) {
        return (T) view.findViewById(id);
    }

    public static <T> T getView(Activity view, int id) {
        return (T) view.findViewById(id);
    }

    public static byte[] decrypt(byte[] encryptedData, byte[] symKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = null;
        // decrypt
        cipher = Cipher.getInstance("AES");
        SecretKey secKey = new SecretKeySpec(symKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secKey);
        byte[] unencryptedObj = cipher.doFinal(encryptedData);
        // unserialize
        return unencryptedObj;
    }

    public static KeyPair KeyPairGeneratorWithRSA() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        return keyGen.generateKeyPair();
    }

    public static void saveKeyPair(String path, KeyPair keyPair) throws IOException {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(path + "/public.key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                privateKey.getEncoded());
        fos = new FileOutputStream(path + "/private.key");
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static String hashWithSHA(String dataToHash) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        Charset ch = Charset.forName("ISO-8859-1");
        return hashWithSHA(dataToHash, ch);
    }

    public static String hashWithSHA(String dataToHash, Charset ch) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        MessageDigest SHA = MessageDigest.getInstance("SHA1");
        SHA.update(dataToHash.getBytes(ch));

        return new String((Hex.encodeHex(SHA.digest())));
    }

    public static String hashWithSHA(InputStream stream) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        MessageDigest SHA = MessageDigest.getInstance("SHA1");
        DigestInputStream dis = new DigestInputStream(stream, SHA);
        byte[] buffer = new byte[5000];
        try {
            while (dis.read(buffer) != -1) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String((Hex.encodeHex(SHA.digest())));
    }

    public static String hashWithSHA(byte[] dataToHash) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        MessageDigest SHA = MessageDigest.getInstance("SHA1");
        SHA.update(dataToHash);
        return new String((Hex.encodeHex(SHA.digest())));
    }

    public static String assymetricDecryptWithRSA(String dataToEncrypt, PrivateKey privateKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, DecoderException {
        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Hex.decodeHex(dataToEncrypt.toCharArray())));
    }

    public static String assymetricEncryptWithRSA(String dataToEncrypt, PublicKey publicKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return new String(Hex.encodeHex(cipher.doFinal(dataToEncrypt.getBytes())));
    }

    /*
            Example of encrypt and decrypt

            PublicKey publicKey = Utils.readPublicKeyFromFile(getContext(),"SecAwarePublicKey.key");
            PrivateKey privateKey = Utils.readPrivateKeyFromFile(getContext(),"SecAwarePrivateKey.key");

            String cipher = Utils.assymetricEncryptWithRSA("BittonRon",publicKey);
            String data = Utils.assymetricDecryptWithRSA(cipher,privateKey);
            Logger.i(getClass(),data);
     */

    public static byte[] assymetricEncryptWithRSA(byte[] dataToEncrypt, Key publicKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(dataToEncrypt);
        return encryptedData;
    }

    public static PrivateKey getPrivateKey(byte[] prKeyBArr) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(prKeyBArr);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    public static byte[] decryptWithPrivate(byte[] encrypted, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encrypted);
    }

    public static byte[] encryptWithAESKey(byte[] data, byte[] key) {
        // create key
        SecretKey secKey = new SecretKeySpec(key, "AES");
        Cipher cipher = null;

        // get alg. instance
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);
        } catch (NoSuchPaddingException e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);
        }

        // init
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secKey);
        } catch (InvalidKeyException e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);
        }

        // encrypt
        try {
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);

        } catch (BadPaddingException e) {
            Logger.e(Utils.class, e.getLocalizedMessage(), e);
        }
        return null;
    }

    public static PublicKey readPublicKeyFromFile(Context context, String fileName) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream stream = context.getAssets().open(fileName);
        byte[] encodedPublicKey = IOUtils.toByteArray(stream);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PrivateKey readPrivateKeyFromFile(Context context, String fileName) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream stream = context.getAssets().open(fileName);
        byte[] encodedPrivateKey = IOUtils.toByteArray(stream);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    public static boolean isBetterThenBestLocation(Location bestLocation, Location newLocation, BigDecimal maxAge, long currentTime) throws Throwable {
        if (bestLocation == null) {
            return true;
        }

        long newLocationAge = currentTime - (newLocation.getTime()) / 1000;
        long bestLocationAge = currentTime - (bestLocation.getTime()) / 1000;

        if (newLocationAge < maxAge.doubleValue() && bestLocationAge < maxAge.doubleValue()) {
            return bestLocation.getAccuracy() > newLocation.getAccuracy();
        } else if (newLocationAge < bestLocationAge) {
            return true;
        }
        return false;
    }

    public static String getConfigurationFolder(Context context) {
        int id = context.getResources().getIdentifier("configFolder", "string", context.getPackageName());
        return (String) context.getResources().getText(id);
    }

    public static String getEmail(Context context, Application app) {
        int id = context.getResources().getIdentifier("appEmail", "string", context.getPackageName());
        return (String) context.getResources().getText(id);
    }

    public static String readFully(InputStream inputStream, String encoding)
            throws IOException {
        return new String(readFully(inputStream), encoding);
    }

    private static byte[] readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
    }

    public static boolean fileZipping(String prevPath, String prevName, String destPath) {
        byte[] buffer = new byte[1024];
        try {
            FileOutputStream fos = new FileOutputStream(destPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(prevName);
            zos.putNextEntry(ze);

            FileInputStream in = new FileInputStream(prevPath);

            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            in.close();
            zos.closeEntry();
            zos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();

            Log.e("readying data to send ", "Error zipping the file : " + e.getMessage());
            return false;

        }
    }

    public static boolean multipaleFileZipping(String[] srcFiles, String zipFile) {
        try {
            // create byte buffer
            byte[] buffer = new byte[1024];

            FileOutputStream fos = new FileOutputStream(zipFile);

            ZipOutputStream zos = new ZipOutputStream(fos);

            for (int i = 0; i < srcFiles.length; i++) {

                File srcFile = new File(srcFiles[i]);

                FileInputStream fis = new FileInputStream(srcFile);

                // begin writing a new ZIP entry, positions the stream to the start of the entry data
                zos.putNextEntry(new ZipEntry(srcFile.getName()));

                int length;

                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();

                // close the InputStream
                fis.close();

            }

            // close the ZipOutputStream
            zos.close();
            return true;
        } catch (IOException ioe) {
            Log.i("Zipping", "Error creating zip file: " + ioe);
            return false;
        }

    }
}
