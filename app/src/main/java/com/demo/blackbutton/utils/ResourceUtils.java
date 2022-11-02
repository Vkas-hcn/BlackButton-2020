package com.demo.blackbutton.utils;


import static com.demo.blackbutton.utils.StringUtils.EMPTY;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <pre>
 *     desc   : 资源工具类
 * </pre>
 */
public final class ResourceUtils {

    private static final String LINE_BREAK = "\r\n";// 换行符

    /**
     * Don't let anyone instantiate this class.
     */
    private ResourceUtils() {
        throw new Error("Do not need instantiate!");
    }

    /**
     * 读取assert下的txt文件
     *
     * @param fileName 文件名
     * @return
     */
    public static String readStringFromAssert(String fileName) {
        return readStringFromAssert(fileName, "utf-8");
    }

    /**
     * 读取assert下的txt文件
     *
     * @param fileName     文件名
     * @param encodingCode 字符编码
     * @return
     */
    public static String readStringFromAssert(String fileName, String encodingCode) {
        InputStream inputStream = null;
        try {
            inputStream = openAssetsFile(fileName);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            return new String(buffer, encodingCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(inputStream);
        }
        return "";
    }

    /**
     * 打开Assets下的文件
     *
     * @param fileName
     * @return
     */
    public static InputStream openAssetsFile(String fileName) {
        try {
            return getAssetManager().open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打开Assets下的文件
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static InputStream openAssetsFileWithException(String fileName) throws IOException {
        return getAssetManager().open(fileName);
    }

    /**
     * 打开Raw下的资源
     *
     * @param resId
     * @return
     */
    public static InputStream openRawResource(int resId) {
        return ResUtils.getResources().openRawResource(resId);
    }

    /**
     * 获取AssetManager
     *
     * @return
     */
    public static AssetManager getAssetManager() {
        return ResUtils.getResources().getAssets();
    }

    /**
     * 获取Assets下文件的内容
     *
     * @param fileName 文件名
     * @return
     */
    public static String getFileFromAssets(String fileName) {
        return getFileFromAssets(fileName, true);
    }

    /**
     * 获取Assets下文件的内容
     *
     * @param fileName      文件名
     * @param isNeedAddLine 是否需要换行
     * @return
     */
    public static String getFileFromAssets(String fileName, boolean isNeedAddLine) {
        if (StringUtils.isEmpty(fileName)) {
            return EMPTY;
        }
        return readInputStream(openAssetsFile(fileName), isNeedAddLine);
    }


    /**
     * 读取raw下文件的内容
     *
     * @param resId 文件资源id
     * @return
     */
    public static String getFileFromRaw(int resId) {
        return getFileFromRaw(resId, true);
    }

    /**
     * 读取raw下文件的内容
     *
     * @param resId         文件资源id
     * @param isNeedAddLine 是否需要换行
     * @return
     */
    public static String getFileFromRaw(int resId, boolean isNeedAddLine) {
        return readInputStream(openRawResource(resId), isNeedAddLine);
    }

    /**
     * 读取输入流
     *
     * @param inputStream   输入流
     * @param isNeedAddLine 是否需要换行
     * @return
     */
    public static String readInputStream(InputStream inputStream, boolean isNeedAddLine) {
        StringBuilder s = new StringBuilder(EMPTY);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            if (isNeedAddLine) {
                while ((line = br.readLine()) != null) {
                    s.append(line).append(LINE_BREAK);
                }
            } else {
                while ((line = br.readLine()) != null) {
                    s.append(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(br);
        }
        return s.toString();
    }
}

