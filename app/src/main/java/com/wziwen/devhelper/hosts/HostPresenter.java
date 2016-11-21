package com.wziwen.devhelper.hosts;

import android.util.Log;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;
import com.wziwen.devhelper.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by wen on 2016/9/25.
 */

public class HostPresenter {

    private static final String TAG = "HostPresenter";
    private static final String SYSTEM_HOST_FILE_PATH = "/system/etc/hosts";


    private static final String MOUNT_TYPE_RO = "ro";
    private static final String MOUNT_TYPE_RW = "rw";
    private static final String COMMAND_RM = "rm -f";
    private static final String COMMAND_CHOWN = "chown 0:0";
    private static final String COMMAND_CHMOD_644 = "chmod 644";

    public void onUpdateHost() {
        downloadFile();
    }

    public void requestRootPremission() throws IOException {
        Process p = Runtime.getRuntime().exec("su");
    }

    public void backupHostFile() throws IOException {
        copy(new File(SYSTEM_HOST_FILE_PATH), new File(Config.getDownloadDir() + "host_backup_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date())));
    }

    public void replaceHostFile(String hostFile) throws IOException {
        File file = new File(SYSTEM_HOST_FILE_PATH);
        Runtime.getRuntime().exec("rm -f " + file.getAbsolutePath());
        copy(new File(Config.getDownloadDir() + hostFile), file);
    }

    public void downloadFile() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com")
                .build();
        HostNetService downloadService = retrofit.create(HostNetService.class);

        String url = "https://raw.githubusercontent.com/racaljk/hosts/master/hosts";
        Call<ResponseBody> call = downloadService.downloadFile(url);

        final String fileName = "host_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Log.d(TAG, "server contacted and has file");
                    boolean writtenToDisk = writeResponseBodyToDisk(fileName, response.body());

                    Log.d(TAG, "file download was a success? " + writtenToDisk);
                    if (writtenToDisk) {
                        try {
                            requestRootPremission();
                            backupHostFile();

                            runRootCommand(COMMAND_RM, SYSTEM_HOST_FILE_PATH);

                            saveHosts(Config.getDownloadDir() + fileName);
//                            copy(new File(Config.getDownloadDir() + fileName), new File(SYSTEM_HOST_FILE_PATH));
//                            RootTools.copyFile(Config.getDownloadDir() + fileName, SYSTEM_HOST_FILE_PATH, false, true);

                            // Step 5: Give proper rights
//                            runRootCommand(COMMAND_CHOWN, SYSTEM_HOST_FILE_PATH);
//                            runRootCommand(COMMAND_CHMOD_644, SYSTEM_HOST_FILE_PATH);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.d(TAG, "server contact failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "error");
            }
        });
    }


    private boolean writeResponseBodyToDisk(String fileName, ResponseBody body) {
        try {
            File targetFile = new File(Config.getDownloadDir() + fileName);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(targetFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public void mountSystem() throws IOException {
        RootTools.remount(SYSTEM_HOST_FILE_PATH, "rw");
//        Runtime.getRuntime().exec("mount -o remount,rw /system");
    }

    public void unmountSystem() throws IOException {
        RootTools.remount(SYSTEM_HOST_FILE_PATH, "ro");
//        Runtime.getRuntime().exec("mount -o remount,ro /system");
    }

    /**
     * Executes a single argument root command.
     * <p><b>Must be in an async call.</b></p>
     *
     * @param command   a command, ie {@code "rm -f"}, {@code "chmod 644"}...
     * @param uniqueArg the unique argument for the command, usually the file name
     */
    private void runRootCommand(String command, String uniqueArg) throws IOException, TimeoutException, RootDeniedException {
        Command cmd = new Command(0, false, String.format(Locale.US, "%s %s", command, uniqueArg));
        RootShell.getShell(true).add(cmd);
    }

    public synchronized boolean saveHosts(String newFilePath) {
        if (!RootTools.isAccessGiven()) {
            Log.w(TAG, "Can't get root access");
            return false;
        }

        // Step 2: Get canonical path for /etc/hosts (it could be a symbolic link)
        String hostsFilePath = SYSTEM_HOST_FILE_PATH;
        File hostsFile = new File(hostsFilePath);

        try {
            RootTools.remount(hostsFilePath, MOUNT_TYPE_RW);

            // Step 4: Replace hosts file with generated file
            runRootCommand(COMMAND_RM, hostsFilePath);
            RootTools.copyFile(newFilePath, hostsFilePath, false, true);

            // Step 5: Give proper rights
            runRootCommand(COMMAND_CHOWN, hostsFilePath);
            runRootCommand(COMMAND_CHMOD_644, hostsFilePath);

        } catch (Exception e) {
            Log.w(TAG, "Failed running root command");
            e.printStackTrace();
            return false;
        } finally {
            RootTools.remount(hostsFilePath, MOUNT_TYPE_RO);
        }
        return true;
    }
}
